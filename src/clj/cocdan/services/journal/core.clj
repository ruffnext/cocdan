(ns cocdan.services.journal.core
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.core.async :refer [go]]
            [clojure.tools.logging :as log]
            [cocdan.core.ops.core :as op-core]
            [cocdan.aux :as data-aux :refer [get-current-time-string]]
            [cocdan.data.core :refer [default-diff']]
            [cocdan.db.monad-db :as monad-db]
            [cocdan.hooks :as hooks]
            [cocdan.services.journal.db :as journal-db :refer [update-ctx!]]
            [cocdan.services.stage.core :as stage-core]
            [datascript.core :as d]))

(defonce transaction-dispatcher (atom {}))

;; 清空 transaction-handler，服务端不对任何 transaction 进行处理
;; 除了内建的 context 的钩子以外
(reset! op-core/transaction-handler {})

"注册日志处理函数，输入参数为
   [register-key stage-id transact, ctx]"
(defn register-journal-hook 
  [stage-id register-key handler-fn] 
  (log/debug (str "HOOK" register-key handler-fn))
  (swap! transaction-dispatcher #(assoc-in % [(keyword (str stage-id)) register-key] handler-fn)))

(defn- m-transact-inner!
  [stage type props]
  (let [stage-journal-atom (journal-db/get-stage-journal-atom stage)]
    (locking stage-journal-atom  ;; stage-journal-atom 是配分各种 id 的 atom，只要保证不重复分配 id，后面就不会出现线程不安全
      (let [{:keys [transaction-id ctx_id ctx]} ((cond    ;; 分配 ctx_id 和 transaction-id
                                                   (= type op-core/OP-SNAPSHOT) journal-db/dispatch-new-ctx_id
                                                   (= type op-core/OP-UPDATE) journal-db/dispatch-new-ctx_id
                                                   :else journal-db/dispatch-new-transaction-id) stage)
            op (op-core/make-transaction transaction-id ctx_id (get-current-time-string) type props true) 
            [transact new-context] (op-core/ctx-generate-ds stage op ctx)]
        (when new-context
          (update-ctx! stage new-context))
        (go ;; 在协程中持久化数据
          (monad-db/persistence-transaction! (assoc (data-aux/remove-db-prefix transact) :stage stage))
          (when new-context
            (monad-db/persistence-context! (assoc (data-aux/remove-db-prefix new-context) :stage stage)))
          (let [handler-map ((keyword (str stage)) @transaction-dispatcher)]
            (doseq [[k f] handler-map]
              (f k stage transact ctx))))
        (either/right transact)))))

(defn m-transact!
  "为外部调用设计的接口，会进行严格的检查"
  [stage type props]
  (m/mlet
   [_check-op (op-core/m-final-validate-transaction {:type type :props props})
    transact-result (m-transact-inner! stage type props)]
   (either/right
    (data-aux/remove-db-prefix transact-result))))

(defn service-transact
  [avatar-id type props access-user-id]
  (m/mlet
   [{:keys [controlled_by stage]} (monad-db/get-avatar-by-id avatar-id)
    _check-user-access (if (= controlled_by access-user-id)
                         (either/right)
                         (either/left "你无权控制该角色"))]
   (m-transact! stage type props)))

(defn m-speak
  [avatar-id props access-user-id]
  (m/mlet
   [{:keys [controlled_by stage]} (monad-db/get-avatar-by-id avatar-id)
    _check-user-access (if (= controlled_by access-user-id)
                         (either/right)
                         (either/left "你无权控制该角色"))] 
   (m-transact! stage "speak" (assoc props :avatar avatar-id))))

(defn- query-stage-contexts
  [stage-id ctx_ids]
  (log/debug ctx_ids)
  (->> ctx_ids
       (map (partial monad-db/get-stage-context-by-id stage-id))
       (either/rights)
       (map m/extract)
       (map data-aux/remove-db-prefix)
       (map #(assoc % :ack true))))

(defn list-transactions
  [stage-id offset limit with-context order]
  (m/mlet
   [_stage (monad-db/get-stage-by-id stage-id)
    transactions (monad-db/list-stage-transactions stage-id order limit offset)]
   (let [transactions (map #(assoc % :ack true) transactions) 
         ctx_ids (->> transactions
                      (map :ctx_id transactions)
                      (filter pos-int?) set)
         min-ctx_ids (apply min ctx_ids)
         contexts (if with-context
                    (query-stage-contexts stage-id ctx_ids)
                    (query-stage-contexts stage-id (if (= min-ctx_ids 0) [1] [min-ctx_ids])))]
     (either/right
      {:transaction (sort-by :id transactions)
       :context (sort-by :id contexts)}))))

(defn- make-stage-snapshot
  [stage]
  (m-transact-inner! (:id stage) op-core/OP-SNAPSHOT stage))

(defn- m-make-update!
  [stage-id diffs]
  (if (empty? diffs)
    (either/right (str stage-id " 没有变化，不生成 update 指令"))
    (m-transact-inner! stage-id op-core/OP-UPDATE diffs)))

#_{:clj-kondo/ignore [:unused-private-var]}
(defn- make-stage-snapshot-by-id
  [stage-id]
  (m/mlet
   [stage (stage-core/query-stage-by-id stage-id)]
   (make-stage-snapshot stage)))

(defn- hook-stage-changed
  [stage-before stage-after]
  (let [diff (stage-before stage-after)]
    (m-make-update! (:id stage-after) diff)))

(defn- hooks-stage-created
  [stage]
  (make-stage-snapshot stage))

(defn- hook-avatar-created
  [{stage :stage :as avatar}]
  (when (not= stage 0)
    (let [res (default-diff' {} {:avatars {(keyword (str (:id avatar))) avatar}})]
      (m-make-update! stage res))))

(defn- hook-avatar-updated
  [{stage-before :stage :as avatar-before} {stage-after :stage :as avatar-after}]
  (let [avatar-key (keyword (str (:id avatar-before)))
        val-before {:avatars {avatar-key avatar-before}}
        val-after {:avatars {avatar-key avatar-after}}]
    (if (= stage-before stage-after)
      (let [diff-before (default-diff' val-before {})
            diff-after (default-diff' {} val-after)]
        (m-make-update! stage-before diff-before)
        (m-make-update! stage-after diff-after))
      (let [diffs (default-diff' val-before val-after)]
        (m-make-update! stage-after diffs)))))

(hooks/hook! :event/after-stage-created :journal-hook hooks-stage-created)
(hooks/hook! :event/after-stage-changed :journal-hook hook-stage-changed)
(hooks/hook! :event/after-avatar-created :journal-hook hook-avatar-created)
(hooks/hook! :event/after-avatar-updated :journal-hook hook-avatar-updated)

(comment
  (let [db (d/create-conn)]
    (d/transact! db [{:db/id 2 :name "a"}])
    (d/transact! db [{:name "b"}])
    @db))
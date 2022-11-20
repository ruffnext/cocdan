(ns cocdan.services.journal.core
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.core.async :refer [go]]
            [clojure.tools.logging :as log]
            [cocdan.aux :as data-aux :refer [get-current-time-string]]
            [cocdan.core.ops.core :as op-core]
            [cocdan.data.core :refer [diff']]
            [cocdan.db.monad-db :as monad-db]
            [cocdan.hooks :as hooks]
            [cocdan.services.journal.db :as journal-db :refer [update-ctx!]]
            [cocdan.services.journal.handler]
            [cocdan.services.stage.core :as stage-core]))

(defonce transaction-dispatcher (atom {}))
"注册日志处理函数，输入参数为
   [register-key stage-id transact, ctx]"
(defn register-journal-hook
  [stage-id register-key handler-fn]
  (swap! transaction-dispatcher #(assoc-in % [(keyword (str stage-id)) register-key] handler-fn)))

(defn- m-transact-inner!
  "参数
    * flush-to-database? 由于调用分为内源调用和外源调用：由于数据库发生变化而申请 journal 的变化
   称为内源调用。这种时候并不需要将 context flush 到数据库中。而外源调用，例如用户的指令。在这种
   情况下，就需要将新的 context flush 到数据库中。当前这个方法的效率较低。"
  ([user stage type props]
   (m-transact-inner! user stage type props false))
  ([user stage type props flush-to-database?]
   (let [dispatcher (if (contains? @op-core/context-handler (keyword type))
                      journal-db/dispatch-new-ctx_id
                      journal-db/dispatch-new-transaction-id)
         {:keys [transaction-id ctx_id ctx]} (dispatcher stage)
         op (op-core/make-transaction-v2 transaction-id ctx_id user (get-current-time-string) type props true)]
     (either/branch-right
      (op-core/ctx-run! op ctx)
      (fn [[transact new-context]]
        (when new-context
          (update-ctx! stage new-context))
        (go ;; 在协程中持久化数据
          (monad-db/persistence-transaction! stage transact)
          (when new-context
            (monad-db/persistence-context! stage new-context)
            (when flush-to-database?
              (monad-db/flush-stage-to-database! (:payload new-context))))
          (let [handler-map ((keyword (str stage)) @transaction-dispatcher)]
            (doseq [[k f] handler-map]
              (f k stage transact ctx))))
        (either/right transact))))))

(defn m-transact!
  "为外部调用设计的接口，会进行严格的检查"
  [stage type props user-id]
  (m/mlet
   [_check-op (op-core/m-final-validate-transaction {:type type :payload props})
    transact-result (m-transact-inner! user-id stage type props true)]
   (either/right
    (data-aux/remove-db-prefix transact-result))))

(defn service-transact
  [stage-id type payload access-user-id]
  (m/mlet
   [_stage (monad-db/get-stage-by-id stage-id)]
   (m-transact! stage-id type payload access-user-id)))

(defn m-speak
  [avatar-id props access-user-id]
  (m/mlet
   [{:keys [controlled_by stage]} (monad-db/get-avatar-by-id avatar-id)
    _check-user-access (if (= controlled_by access-user-id)
                         (either/right)
                         (either/left "你无权控制该角色"))]
   (m-transact! access-user-id stage "speak" (assoc props :avatar avatar-id))))

(defn- query-stage-contexts
  [stage-id ctx_ids]
  (->> ctx_ids
       (map (partial monad-db/get-stage-context-by-id stage-id))
       (either/rights)
       (map m/extract)
       (map data-aux/remove-db-prefix)
       (map #(assoc % :ack true))))

(defn list-transactions
  [stage-id begin offset limit with-context order]
  (m/mlet
   [_stage (monad-db/get-stage-by-id stage-id)
    transactions (monad-db/list-stage-transactions stage-id order limit begin offset)]
   (let [transactions (sort-by :id (map #(assoc % :ack true) transactions))
         {last-tid :id last-t-type :type} (last transactions)
         ctx_ids (->> transactions
                      (map :ctx_id transactions)
                      (filter pos-int?) set)
         min-ctx_ids (apply min ctx_ids)
         contexts (if with-context
                    (query-stage-contexts stage-id
                                          (-> ctx_ids
                                              (#(if (contains? @op-core/context-handler (keyword last-t-type))
                                                  (conj % last-tid) %))))
                    (query-stage-contexts stage-id (if (= min-ctx_ids 0) [1] [min-ctx_ids])))]
     (either/right
      {:transaction (sort-by :id transactions)
       :context (sort-by :id contexts)}))))

(defn- handle-empty-context
  [{:keys [id] :as stage}]
  (let [current-time-string (get-current-time-string)]
    (m/>>
     (monad-db/persistence-context!
      id (op-core/make-context-v2 1 current-time-string stage true))
     (monad-db/persistence-transaction!
      id (op-core/make-transaction-v2 1 1 0 current-time-string "noop" {} true)))))

(defn- make-stage-snapshot
  [stage]
  (m-transact-inner! 0 (:id stage) op-core/OP-SNAPSHOT stage))

(defn- m-make-update!
  [stage-id diffs]
  (if (empty? diffs)
    (either/right (str stage-id " 没有变化，不生成 update 指令"))
    (m-transact-inner! 0 stage-id op-core/OP-UPDATE diffs)))

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

(defn- hook-avatar-created
  [{stage :stage :as avatar}]
  (when (not= stage 0)
    (let [res (diff' {} {:avatars {(keyword (str (:id avatar))) avatar}})]
      (m-make-update! stage res))))

(defn- hook-avatar-updated
  [{stage-before :stage :as avatar-before} {stage-after :stage :as avatar-after}]
  (let [avatar-key (keyword (str (:id avatar-before)))
        val-before {:avatars {avatar-key avatar-before}}
        val-after {:avatars {avatar-key avatar-after}}]
    (if (= stage-before stage-after)
      (let [diff-before (diff' val-before {})
            diff-after (diff' {} val-after)]
        (m-make-update! stage-before diff-before)
        (m-make-update! stage-after diff-after))
      (let [diffs (diff' val-before val-after)]
        (m-make-update! stage-after diffs)))))

(hooks/hook! :event/after-stage-created :journal-hook handle-empty-context)
(hooks/hook! :event/after-stage-changed :journal-hook hook-stage-changed)
(hooks/hook! :event/after-avatar-created :journal-hook hook-avatar-created)
(hooks/hook! :event/after-avatar-updated :journal-hook hook-avatar-updated)

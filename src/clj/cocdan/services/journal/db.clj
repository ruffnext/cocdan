(ns cocdan.services.journal.db
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.tools.logging :as log]
            [cocdan.aux :refer [get-current-time-string]]
            [cocdan.core.ops.core :refer [make-context-v2]]
            [cocdan.db.monad-db :as monad-db]))

; 存放舞台最新的 ctx 的 map
; 格式为 {:ctx_id id :transaction-d tid :ctx {:context/id id}}
(defonce db (atom {}))

(defn load-stage-db-from-database
  [stage-id]
  (let [stage-key (keyword (str stage-id))
        stage-db (-> (swap! db (fn [x] (update x stage-key #(or % (atom {})))))
                     stage-key)]
    (locking stage-db 
      (m/mlet
       [{ctx_id :id
         :as latest-ctx} (either/branch-left
                             (monad-db/get-stage-latest-ctx-by-stage-id stage-id)
                             (fn [_] (either/right
                                      (make-context-v2 0 (get-current-time-string) {} true))))
        latest-t-id (either/branch-left
                     (monad-db/get-latest-transaction-id-by-stage-id stage-id)
                     (fn [_] (either/right 0)))]
       (when (= 0 ctx_id)
         (log/warn (str "舞台 " stage-id " 从数据库中初始化上下文失败！")))
       (reset! stage-db {:ctx_id ctx_id
                         :transaction-id latest-t-id
                         :ctx (assoc latest-ctx :ack true)}) 
       stage-db))))

(defn get-stage-journal-atom
  "获得当前舞台的 ctx_id 和 transaction-id
   该函数不会失败，如果读取不到记录，则两个 id 都会设置为 0"
  [stage-id]
  (let [conf ((keyword (str stage-id)) @db)]
    (if (nil? conf)
      (load-stage-db-from-database stage-id)
      conf)))

(defn dispatch-new-transaction-id
  "如果 transaction 不改变上下文，则只分配 transaction id"
  [stage-id]
  (let [stage-conf (get-stage-journal-atom stage-id)]
    (swap! stage-conf #(update % :transaction-id inc))))

(defn dispatch-new-ctx_id
  "如果 transaction 改变上下文，则需要分配 ctx id"
  [stage-id]
  (let [stage-conf (get-stage-journal-atom stage-id)]
    (swap! stage-conf (fn [x]
                        (let [new-id (inc (:transaction-id x))]
                          (-> x    ; 将 ctx_id 和 transaction-id 都更新
                              (assoc :transaction-id new-id)
                              (assoc :ctx_id new-id)))))))

(defn update-ctx!
  [stage-id ctx]
  (let [stage-conf (get-stage-journal-atom stage-id)]
    (swap! stage-conf #(assoc % :ctx ctx))))

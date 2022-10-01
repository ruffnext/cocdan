(ns cocdan.services.journal.db
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.tools.logging :as log]
            [cocdan.data.aux :as data-aux]
            [cocdan.db.monad-db :as monad-db]))

; 存舞台的 ctx_id 、 operation-id 和 ctx_id 对应的 context
(defonce db (atom {}))

(defn- init-stage-db
  [stage-id]
  (let [stage-key (keyword (str stage-id))
        stage-db (-> (swap! db (fn [x] (update x stage-key #(or % (atom {})))))
                     stage-key)]
    (locking stage-db 
      (m/mlet
       [{ctx_id :id
         :as latest-ctx} (either/branch-left
                             (monad-db/get-stage-latest-ctx-by-stage-id stage-id)
                             (fn [_] (either/right (data-aux/add-db-prefix 
                                                    :context {:id 0 :stage stage-id :time nil :props {}}))))
        latest-t-id (either/branch-left
                     (monad-db/get-latest-transaction-id-by-stage-id stage-id)
                     (fn [_] (either/right 0)))]
       (when (= 0 ctx_id)
         (log/warn (str "舞台 " stage-id " 从数据库中初始化上下文失败！")))
       (reset! stage-db {:ctx_id ctx_id
                         :transaction-id latest-t-id
                         :ctx latest-ctx}) 
       stage-db))))

(defn get-stage-journal-atom
  "获得当前舞台的 ctx_id 和 transaction-id
   该函数不会失败，如果读取不到记录，则两个 id 都会设置为 0"
  [stage-id]
  (let [conf ((keyword (str stage-id)) @db)]
    (if (nil? conf)
      (init-stage-db stage-id)
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
                          (-> x
                              (assoc :transaction-id new-id)
                              (assoc :ctx_id new-id)))))))

(defn update-ctx!
  [stage-id ctx]
  (let [stage-conf (get-stage-journal-atom stage-id)]
    (swap! stage-conf #(assoc % :ctx ctx))))

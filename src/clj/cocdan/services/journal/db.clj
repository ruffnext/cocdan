(ns cocdan.services.journal.db 
  (:require [cats.core :as m]
            [cocdan.db.monad-db :as monad-db]
            [cats.monad.either :as either]
            [clojure.tools.logging :as log]))

; 存舞台的 ctx-id 、 operation-id 和 ctx-id 对应的 context
(defonce db (atom {}))

(defn- init-stage-db
  [stage-id]
  (let [stage-key (keyword (str stage-id))
        stage-db (-> (swap! db (fn [x] (update x stage-key #(or (stage-key x) (atom {})))))
                     stage-key)]
    (locking stage-db
      (m/mlet
       [{ctx-id :id
         :as latest-ctx} (either/branch-left
                          (monad-db/get-stage-latest-ctx-by-stage-id stage-id)
                          (fn [_] (either/right {:id 0 :stage stage-id :time nil :props {}})))
        latest-t-id (either/branch-left
                     (monad-db/get-latest-transaction-id-by-stage-id stage-id)
                     (fn [_] (either/right 0)))]
       (when (= 0 ctx-id)
         (log/warn (str "舞台 " stage-id " 从数据库中初始化上下文失败！")))
       (reset! stage-db {:ctx-id ctx-id
                         :transaction-id latest-t-id
                         :ctx latest-ctx})
       stage-db))))

(defn get-stage-id-map
  "获得当前舞台的 ctx-id 和 transaction-id
   该函数不会失败，如果读取不到记录，则两个 id 都会设置为 0"
  [stage-id]
  (let [conf ((keyword (str stage-id)) @db)]
    (if (nil? conf)
      (init-stage-db stage-id)
      conf)))

(defn dispatch-new-transaction-id
  "如果 transaction 不改变上下文，则只分配 transaction id"
  [stage-id]
  (let [stage-conf (get-stage-id-map stage-id)]
    (swap! stage-conf #(update % :transaction-id inc))))

(defn dispatch-new-ctx-id
  "如果 transaction 改变上下文，则需要分配 ctx id"
  [stage-id]
  (let [stage-conf (get-stage-id-map stage-id)]
    (swap! stage-conf (fn [x] 
                        (let [new-id (inc (:transaction-id x))]
                          {:transaction-id new-id
                           :ctx-id new-id})))))

(defn update-ctx!
  [stage-id ctx]
  (let [stage-conf (get-stage-id-map stage-id)]
    (swap! stage-conf #(assoc % :ctx ctx))))

(comment
  (let [tmp (atom {})]
    (swap! tmp #(assoc % :k 1)))

  )
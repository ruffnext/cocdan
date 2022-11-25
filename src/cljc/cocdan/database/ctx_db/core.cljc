(ns cocdan.database.ctx-db.core
  (:require [cocdan.data.stage :refer [new-stage]]
            [datascript.core :as d]
            [malli.core :as mc]
            [cocdan.core.ops.data :as op-data]))

;; 注意：这是缓存数据库，可能存在不一致的情况

(def cache-database-schema
  "缓存 transaction 和 context 的数据库"
  {:play/id {:db/unique :db.unique/identity}
   :transaction/id {:db/unique :db.unique/identity}
   :context/id {:db/unique :db.unique/identity}})

; {:stage-id-a @{:db history-db :latest {:ctx context :ctx_id id :transaction_id id}} :stage-id-b db ...}
(defonce db (atom {}))

(defn- make-db
  [stage-id]
  (let [new-db (atom {:history-db (d/create-conn cache-database-schema)
                      :latest {:context nil :ctx_id 0 :transaction_id 0}
                      :stage-key (keyword (str stage-id))})]
    (swap! db (fn [x] (assoc x (keyword (str stage-id)) new-db)))
    new-db))

(defn dispatch!
  "返回 {:ctx context :ctx_id id :transaction_id id}}"
  [stage-db verified?]
  (:latest (if verified?
             (swap! stage-db (fn [x] (update-in x [:latest :transaction_id] inc)))
             (update-in @stage-db [:latest :transaction_id] inc))))

(defn- update-latest-transaction!
  [stage-db transactions]
  (let [max-tid-verified (->> (filter :ack transactions)
                              (map :id)
                              (apply max 0))]
    (when (> max-tid-verified (-> @stage-db :latest :transaction_id))
      (swap! stage-db (fn [x] (assoc-in x [:latest :transaction_id] max-tid-verified))))))

(defn- update-latest-context!
  [stage-db contexts]
  (let [{:keys [id] :as latest-verified-context}
        (->> (filter :ack contexts)
             (sort-by :id)
             last)]
    (when (and latest-verified-context (> id (-> @stage-db :latest :ctx_id)))
      (swap! stage-db (fn [x] (-> x
                                  (assoc-in [:latest :ctx] latest-verified-context)
                                  (assoc-in [:latest :ctx_id] id)))))))

(defn insert-transactions
  [stage-db transactions]
  (let [ds-record (for [{:keys [id] :as transaction} transactions]
                    (assoc transaction :transaction/id id))]
    (update-latest-transaction! stage-db transactions)
    (d/transact! (:history-db @stage-db) (vec ds-record)))

  nil)
(mc/=> insert-transactions [:=> [:cat :any [:vector op-data/transaction-spec]] nil?])

(defn insert-contexts
  [stage-db contexts]
  (let [ds-record (for [{:keys [id] :as context} contexts]
                    (assoc context :context/id id))]
    (update-latest-context! stage-db contexts)
    (d/transact! (:history-db @stage-db) (vec ds-record)) nil))
(mc/=> insert-contexts [:=> [:cat :any [:vector op-data/context-spec]] nil?])

(defn query-stage-db
  "获得描述舞台的数据库
   如果这个舞台尚未初始化，则返回一个空的数据库"
  [stage-id]
  (let [stage-db ((keyword (str stage-id)) @db)]
    (if stage-db
      stage-db
      (make-db stage-id))))

(defn query-latest-ctx
  [stage-db]
  (get-in @stage-db [:latest :ctx]))

(defn query-context-by-id
  "取得缓存的上下文记录，该函数有可能会失败！"
  [stage-db ctx_id]
  (let [ds @(:history-db @stage-db)]
    (if
     (= 0 ctx_id)
      nil
      (let [res (d/pull ds '[*] [:context/id ctx_id])]
        (-> (dissoc res :db/id)
            (dissoc res :context/id))))))

(defn query-ds-ctx-by-id
  "取得上下文。该函数可能会失败！"
  [ds ctx_id]
  (if
   (= 0 ctx_id)
    nil
    (let [res (d/pull ds '[*] [:context/id ctx_id])]
      (-> (dissoc res :db/id)
          (dissoc res :context/id)))))

(defn query-ds-latest-ctx
  [ds]
  (let [eid (->> (d/datoms ds :avet :context/id)
                 reverse first first)]
    (when eid
      (-> eid
          (->> (d/pull ds '[*]))
          (dissoc :db/id)
          (dissoc :context/id)))))

(defn query-latest-transaction-id
  [stage-db]
  (get-in @stage-db [:latest :transaction_id]))

(defn import-stage-context!
  [stage-db contexts]
  (let [context (->> contexts
                     (map #(update % :payload new-stage))
                     (map #(assoc % :context/id (:id %))))]
    (d/transact! (:history-db @stage-db) context)
    (update-latest-context! stage-db context)))
(mc/=> import-stage-context! [:=> [:cat :any [:vector op-data/context-spec]] :any])

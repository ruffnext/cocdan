(ns cocdan.database.ctx-db.core 
  (:require [cocdan.data.core :as data-core]
            [cocdan.data.stage :refer [new-stage]]
            [datascript.core :as d]
            [malli.core :as mc]
            [cocdan.core.ops.data :as op-data]))

;; 注意：这是缓存数据库，可能存在不一致的情况

(def cache-database-schema
  "缓存 transaction 和 context 的数据库"
  {:play/id {:db/unique :db.unique/identity}
   :transaction/id {:db/unique :db.unique/identity}
   :context/id {:db/unique :db.unique/identity}})

(defonce db (atom {}))

(defn insert-transactions
  [ds-db transactions]
  (let [ds-record (for [{:keys [id] :as transaction} transactions]
                    (assoc transaction :transaction/id id))]
    (d/transact! ds-db (vec ds-record))) nil)
(mc/=> insert-transactions [:=> [:cat :any [:vector op-data/transaction-spec]] nil?])

(defn insert-contexts
  [ds-db contexts]
  (let [ds-record (for [{:keys [id] :as context} contexts]
                    (assoc context :context/id id))] 
    (d/transact! ds-db (vec ds-record)) nil))
(mc/=> insert-contexts [:=> [:cat :any [:vector op-data/context-spec]] nil?])

(defn query-stage-db
  "获得描述舞台的 DataScript 数据库
   如果这个舞台尚未初始化，则返回一个空的数据库"
  [stage-id]
  (let [stage-key (keyword (str stage-id))
        stage-db (stage-key @db)]
    (if stage-db
      stage-db
      (let [new-db (d/create-conn cache-database-schema)]
        (swap! db (fn [x] (assoc x stage-key new-db)))
        new-db))))

(defn query-ds-ctx-by-id
  "取得上下文。该函数可能会失败！"
  [ds ctx_id]
  (if
   (= 0 ctx_id)
    {}
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

;; (defn query-ds-latest-ctx_id
;;   [ds]
;;   (->> (d/datoms ds :avet :context/id)
;;        reverse first first
;;        (d/pull ds '[:context/id])
;;        :context/id))

(defn query-ds-latest-transaction-id
  ([ds]
   (let [datoms (d/datoms ds :avet :transaction/id)]
     (when datoms
       (->> datoms
            reverse first first
            (d/pull ds '[:transaction/id])
            :transaction/id))))
  ([ds verified?]
   (if verified?
     (let [datoms (d/datoms ds :avet :transaction/id)]
       (->> datoms
            reverse (map first)
            (d/pull-many ds '[:transaction/id :ack])
            (drop-while #(not (:ack %)))
            first :transaction/id))
     (query-ds-latest-transaction-id ds))))

(defn import-stage-context!
  [stage-db contexts] 
  (let [context (->> contexts
                     (map #(update % :payload new-stage))
                     (map #(assoc % :context/id (:id %))))] 
    (d/transact! stage-db context)))
(mc/=> import-stage-context! [:=> [:cat :any [:vector op-data/context-spec]] :any])

(defn query-ds-latest-transaction
  [ds]
  (->> (d/datoms ds :avet :transaction/id)
       reverse first first
       (d/pull ds '[:transaction/props])
       :transaction/props))

(defn query-latest-ctx-by-stage-id
  [stage-id]
  (let [ds @(query-stage-db stage-id)]
    (query-ds-latest-ctx ds)))

(defn check-consistency
  [ds contents]
  (let [ctx (query-ds-latest-ctx ds)]
    (data-core/diff' (:props ctx) contents)))


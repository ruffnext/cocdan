(ns cocdan.database.ctx-db.core 
  (:require [cocdan.data.core :as data-core]
            [cocdan.data.stage :refer [new-stage]]
            [cocdan.database.schemas :refer [play-room-database-schema]]
            [datascript.core :as d]
            [cocdan.aux :as data-aux]))

;; 注意：这是缓存数据库，可能存在不一致的情况

(defonce db (atom {}))

(defn query-stage-db
  "获得描述舞台的 DataScript 数据库
   如果这个舞台尚未初始化，则返回一个空的数据库"
  [stage-id]
  (let [stage-key (keyword (str stage-id))
        stage-db (stage-key @db)]
    (if stage-db
      stage-db
      (let [new-db (d/create-conn play-room-database-schema)]
        (swap! db (fn [x] (assoc x stage-key new-db)))
        new-db))))

(defn query-ds-ctx-by-id
  "取得上下文。该函数可能会失败！"
  [ds ctx_id]
  (let [res (d/pull ds '[*] [:context/id ctx_id])]
    (dissoc res :db/id)))

(defn query-ds-latest-ctx
  [ds]
  (let [eid (->> (d/datoms ds :avet :context/id)
                 reverse first first)]
    (when eid
      (->> eid
           (d/pull ds '[*])
           (#(dissoc % :db/id))))))

(defn query-ds-latest-ctx_id
  [ds]
  (->> (d/datoms ds :avet :context/id)
       reverse first first
       (d/pull ds '[:context/id])
       :context/id))

(defn query-ds-latest-transaction-id
  [ds]
  (->> (d/datoms ds :avet :transaction/id)
       reverse first first
       (d/pull ds '[:transaction/id])
       :transaction/id))

(defn import-stage-context!
  [stage-db contexts]
  (let [context (->> contexts
                     (map #(update % :props new-stage))
                     (map #(data-aux/add-db-prefix :context %)))]
    (d/transact! stage-db context)))

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


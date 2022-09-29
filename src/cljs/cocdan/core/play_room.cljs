(ns cocdan.core.play-room
  (:require [cocdan.core.ops :as core-ops]
            [cocdan.database.schemas :refer [play-room-database-schema]]
            [datascript.core :as d]
            [re-frame.core :as rf]))

(defonce db (atom {}))

(defn- fetch-stage-db
  [stage-id]
  (let [stage-key (keyword (str stage-id))
        stage-db (stage-key @db)] 
    (if stage-db
      stage-db
      (let [new-db (d/create-conn play-room-database-schema)]
        (swap! db (fn [x] (assoc x stage-key new-db)))
        new-db))))

(defn query-stage-db
  [stage-id]
  @(fetch-stage-db stage-id))

(rf/reg-event-fx
 :play/execute
 (fn [{:keys [db]} [_ stage-id ops] ack]
   (let [stage-key (keyword (str stage-id))
         ds-db (fetch-stage-db stage-id)
         max-transact-id-path [:stage stage-key :max-transact-id]
         rf-max-transact-id (or (get-in db max-transact-id-path) 0)
         ops (sort-by first ops)
         last-transact-id (-> ops last first)
         new-db (update-in db [:stage stage-key :refresh] #(if % (inc %) 0))]
     (doseq [op ops]
       (core-ops/ctx-run! ds-db op (or ack false))) 
     (if (> last-transact-id rf-max-transact-id)
       {:db (assoc-in new-db max-transact-id-path last-transact-id)}
       {:db new-db}))))

(rf/reg-sub
 :play/refresh
 (fn [db [_ stage-id]]
   (or (get-in db [:stage (keyword stage-id) :refresh]) 0)))

(rf/reg-sub
 :play/latest-transact-id
 (fn [db [_ stage-id]] 
   (or (get-in db [:stage (keyword stage-id) :max-transact-id]) 0)))
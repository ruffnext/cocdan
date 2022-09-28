(ns cocdan.core.play-room
  (:require [cocdan.core.ops :as core-ops]
            [cocdan.database.schemas :refer [play-room-database-schema]]
            [datascript.core :as d]
            [re-frame.core :as rf]))

(defonce db (atom {}))

(defn fetch-stage-db
  [stage-id]
  (let [stage-key (keyword stage-id)
        stage-db (stage-key db)]
    (if stage-db
      stage-db
      (let [new-db (d/create-conn play-room-database-schema)]
        (swap! db (fn [x] (assoc x stage-key new-db)))
        new-db))))

(rf/reg-event-fx
 :play/execute
 (fn [{:keys [db]} [_ stage-id ops]]
   (let [stage-key (keyword (str stage-id))
         ds-db (fetch-stage-db stage-key)
         max-transact-id-path [:stage stage-key :max-transact-id]
         rf-max-transact-id (or (get-in db max-transact-id-path) 0)])))

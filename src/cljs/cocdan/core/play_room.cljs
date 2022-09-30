(ns cocdan.core.play-room
  (:require [cocdan.core.ops.core :as core-ops]
            [cocdan.database.ctx-db.core :as ctx-db] 
            [re-frame.core :as rf]))

;; 聊天室的核心

(defn query-stage-ds
  [stage-id]
  @(ctx-db/query-stage-db stage-id))

(rf/reg-event-fx
 :play/execute
 (fn [{:keys [db]} [_ stage-id ops] ack]
   (let [stage-key (keyword (str stage-id))
         ds-db (ctx-db/query-stage-db stage-id)
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

(ns cocdan.core.stage
  (:require
   [re-frame.core :as rf]
   [clojure.core.async :refer [go <!]]
   [cljs-http.client :as http]
   [cocdan.auxiliary :as gaux]
   [re-posh.core :as rp]
   [cocdan.db :as gdb]))

(defn- refresh-stage-avatars
  [{stage-id :stage-id}]
  (go (let [my-avatars (first (filter #(= (:on_stage %) stage-id) (gdb/posh-my-avatars gdb/conn)))
            res (<! (http/get (str "/api/stage/s" stage-id "/list/avatar") {:query-params {:id stage-id
                                                                                           :avatar-id (:id my-avatars)}}))]
        (cond
          (= (:status res) 200) 
          (do
            (rp/dispatch-sync [:rpevent/upsert :avatar (:body res)])
            (when (nil? (gdb/posh-current-use-avatar-id gdb/conn stage-id))
              (let [avatars (gdb/posh-my-avatars gdb/conn)
                    avatars-can-use (filter #(= (:on_stage %) stage-id) avatars)]
                (rp/dispatch-sync [:rpevent/upsert :stage {:id stage-id
                                                           :current-use-avatar (:id (first avatars-can-use))}]))))
          :else (js/console.log res)))))

(defn- refresh-stage
  [{stage-id :stage-id}]
  (go (let [res (<! (http/get (str "/api/stage/s" stage-id "")))]
        (cond
          (= (:status res) 200) (rp/dispatch [:rpevent/upsert :stage (:body res)])
          :else (js/console.log res)))))

(defn- update-substage
  [db [_query-id stage-id substage-id keys val]]
  (->> (fn [sub-stage]
         (-> sub-stage
             (assoc-in keys val)
             (assoc :id substage-id)))
       #(fn [stage] (assoc stage
                           :id stage-id
                           :sub-stages (gaux/swap-filter-list-map!
                                        (:sub-stages stage)
                                        (fn [x] (= (:id x) substage-id))
                                        %)))
       ((fn [f]
          (gaux/swap-filter-list-map!
           (:stages db)
           #(= (:id %) stage-id)
           f)))
       (assoc db :stages)))

(defn- get-stage
  [db [_query-id stage-id]]
  (js/console.log _query-id)
  (first (filter #(= (:id %) stage-id) (:stages db))))

(defn- get-stage-avatars
  [db [_query-id stage-id]]
  (filter #(= (:on_stage %) stage-id) (:avatars db)))


(doseq [[sub f] {:subs/stage get-stage
                 :subs/stage-avatars get-stage-avatars}]
  (rf/reg-sub sub f))

(doseq [[fx f] {:event/update-substage update-substage}]
  (rf/reg-event-db fx f))

(doseq [[fx f] {:fx/stage-refresh-avatars refresh-stage-avatars
                :fx/stage-refresh refresh-stage}] 
       (rf/reg-fx fx f))

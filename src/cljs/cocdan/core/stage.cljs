(ns cocdan.core.stage
  (:require
   [re-frame.core :as rf]
   [clojure.core.async :refer [go <!]]
   [cljs-http.client :as http]
   [cocdan.core.avatar :refer [posh-my-avatars posh-current-use-avatar-eid]]
   [re-posh.core :as rp]
   [posh.reagent :as p]
   [cocdan.db :as gdb]))

(defn- refresh-stage-avatars
  [{stage-id :stage-id}]
  (go (let [my-avatars (first (filter #(= (:on_stage %) stage-id) (->> @(posh-my-avatars gdb/db)
                                                                       (gdb/pull-eids gdb/db))))
            res (<! (http/get (str "/api/stage/s" stage-id "/list/avatar") {:query-params {:id stage-id
                                                                                           :avatar-id (:id my-avatars)}}))]
        (cond
          (= (:status res) 200) 
          (do
            (rp/dispatch-sync [:rpevent/upsert :avatar (:body res)])
            (when (nil? @(posh-current-use-avatar-eid gdb/db stage-id))
              (let [avatars-can-use (filter #(= (:on_stage %) stage-id) (->> @(posh-my-avatars gdb/db)
                                                                             (gdb/pull-eids gdb/db)))]
                (rp/dispatch-sync [:rpevent/upsert :stage {:id stage-id
                                                           :current-use-avatar (:id (first avatars-can-use))}]))))
          :else (js/console.log res)))))

(defn- refresh-stage
  [{stage-id :stage-id}]
  (go (let [res (<! (http/get (str "/api/stage/s" stage-id "")))]
        (cond
          (= (:status res) 200) (rp/dispatch [:rpevent/upsert :stage (:body res)])
          :else (js/console.log res)))))


(doseq [[fx f] {:fx/stage-refresh-avatars refresh-stage-avatars
                :fx/stage-refresh refresh-stage}] 
       (rf/reg-fx fx f))

(defn posh-stage-by-id
  [ds stage-id]
  (p/q '[:find [?s-eid]
         :in $ ?stage-id
         :where [?s-eid :stage/id ?stage-id]]
       ds
       stage-id))

(defn posh-am-i-stage-admin?
  [ds stage-id]
  (p/q '[:find ?stage-controller .
         :in $ ?stage-id
         :where
         [_ :my-info/id ?my-id]
         [?stage-eid :stage/id ?stage-id]
         [?stage-eid :stage/owned_by ?stage-controller]
         [?avatar-eid :avatar/controlled_by ?my-id]
         [?avatar-eid :avatar/id ?avatar-id]
         [(= ?avatar-id ?stage-controller)]]
       ds
       stage-id))



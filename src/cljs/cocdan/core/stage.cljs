(ns cocdan.core.stage
  (:require
   [posh.reagent :as p]
   [cljs-http.client :as http]
   [clojure.core.async :refer [go <!]]
   [re-frame.core :as rf]))


(defn posh-stage-by-id
  [db stage-id]
  (p/q '[:find [?s-eid]
         :in $ ?stage-id
         :where [?s-eid :stage/id ?stage-id]]
       db
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

(rf/reg-event-db
 :event/refresh-stage
 (fn [db [_driven-by stage-id]]
   (go (let [res (<! (http/get (str "/api/stage/s" stage-id)))]
         (when (= (:status res) 200)
           (rf/dispatch [:rpevent/upsert :stage (:body res)]))))
   db))

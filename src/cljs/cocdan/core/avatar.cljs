(ns cocdan.core.avatar
  (:require
   [posh.reagent :as p]
   [re-frame.core :as rf]
   [clojure.core.async :refer [go <!]]
   [cljs-http.client :as http]))

(defn posh-my-avatars
  [ds]
  (p/q '[:find [?a-eid ...]
         :where
         [?my-eid :my-info/id ?my-id]
         [?a-eid :avatar/controlled_by ?my-id]]
       ds))

(defn posh-current-use-avatar-eid
  [ds stage-id]
  (p/q '[:find ?aeid .
         :in $ ?stage-id
         :where
         [?seid :stage/id ?stage-id]
         [?seid :stage/current-use-avatar ?aid]
         [?aeid :avatar/id ?aid]]
       ds
       stage-id))

(defn posh-avatars-by-stage-id
  [ds stage-id]
  (p/q '[:find [?a-eid ...]
         :in $ ?stage-id
         :where
         [?a-eid :avatar/on_stage ?stage-id]]
       ds
       stage-id))

(defn posh-avatar-by-id
  [db avatar-id]
  (p/q '[:find ?a-eid .
         :in $ ?avatar-id
         :where [?a-eid :avatar/id ?avatar-id]]
       db
       avatar-id))

(rf/reg-event-db
 :event/refresh-my-avatars
 (fn [db [_driven-by]]
   (go
     (let [res (<! (http/get "/api/user/avatar/list"))]
       (when (= (:status res) 200)
         (let [avatars (-> res :body)
               stage-ids (-> (reduce (fn [a {on_stage :on_stage}]
                                       (if on_stage
                                         (conj a on_stage)
                                         a))
                                     [] avatars)
                             set)]
           (rf/dispatch [:rpevent/upsert :avatar avatars])
           (doseq [stage-id stage-ids]
             (rf/dispatch [:event/refresh-stage stage-id]))))))
   db))
(ns cocdan.core.avatar
  (:require
   [posh.reagent :as p]))

(defn posh-my-avatars
  [ds]
  (p/q '[:find [?a-eid ...]
         :where
         [?my-eid :my-info/id ?my-id]
         [?a-eid :avatar/controlled_by ?my-id]]
       ds))

(defn posh-current-use-avatar-eid
  [ds stage-id]
  (p/q '[:find [?aeid]
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
  [ds avatar-id]
  (p/q '[:find [?a-eid]
         :in $ ?avatar-id
         :where [?a-eid :avatar/id ?avatar-id]]
       ds
       avatar-id))
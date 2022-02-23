(ns cocdan.core.stage
  (:require
   [posh.reagent :as p]))


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



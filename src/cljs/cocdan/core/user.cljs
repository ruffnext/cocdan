(ns cocdan.core.user
  (:require
   [posh.reagent :as p]))

(defn posh-my-eid
  [ds]
  (p/q '[:find [?my-eid ...]
         :where
         [?my-eid :my-info/id _]]
       ds))
(ns cocdan.database.main
  (:require [datascript.core :as d]
            [posh.reagent :as p] 
            [cocdan.database.schemas :refer [main-database-schema]]
            [re-frame.core :as rf]))

(defonce db (d/create-conn main-database-schema))
(p/posh! db)

(rf/reg-event-fx
 :ds/transact-records
 (fn [_ [_ records]] 
   (d/transact! db records)
   {}))

(defn posh-stage-ids
  [] 
  (p/q '[:find [?id ...]
         :where [?e :stage/id ?id]]
       db))

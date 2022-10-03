(ns cocdan.core.settings 
  (:require [cocdan.database.main :refer [db]]
            [datascript.core :as d]
            [posh.reagent :as p]))

(defn update-setting-value-by-key
  [k value]
  (when (keyword? k)
    (d/transact db
                [{:setting/key k :setting/value value}])))

(defn posh-setting-key-and-values
  []
  (p/q '[:find ?k ?v
         :where
         [?e :setting/key ?k]
         [?e :setting/value ?v]]
       db))

(defn query-setting-value-by-key
  [setting-key]
  (:setting/value (d/pull @db '[:setting/value] [:setting/key setting-key])))

(defn init-default-settings
  []
  (update-setting-value-by-key :is-kp true))

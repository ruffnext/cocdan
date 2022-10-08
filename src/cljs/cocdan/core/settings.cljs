(ns cocdan.core.settings 
  (:require [cocdan.database.main :refer [db]]
            [datascript.core :as d]
            [posh.reagent :as p]
            [re-frame.core :as rf]))

(defn update-setting-value-by-key
  [k value]
  (when (keyword? k)
    (d/transact db
                [{:setting/key k :setting/value value}]))
  (rf/dispatch [:chat-log/clear-cache!]))

(defn register-setting-key-value
  [k name value]
  (d/transact db [{:setting/key k  :setting/name name :setting/value value}]))

(defn posh-setting-key-and-values
  []
  (p/q '[:find ?k ?n ?v
         :where
         [?e :setting/key ?k]
         [?e :setting/name ?n]
         [?e :setting/value ?v]]
       db))

(defn query-setting-value-by-key
  [setting-key]
  (:setting/value (d/pull @db '[:setting/value] [:setting/key setting-key])))

(defn init-default-settings
  []
  (register-setting-key-value :game-play/is-kp "启用 KP 模式" false))

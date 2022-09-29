(ns cocdan.fragment.chat-log 
  (:require [cocdan.data.action :as action]
            [cocdan.data.core :refer [get-substage-id ITerritorialMixIn]]
            [cocdan.data.visualizable :as visualizable :refer [IVisualizable]]
            [datascript.core :as d]))

(defn chat-log
  [{:keys [substage ctx-ds viewpoint limit] :or {limit 10}}]
  (let [plays (->> (d/datoms ctx-ds :avet :transact/id)
                   reverse (map first)
                   (d/pull-many ctx-ds [:transact/props])
                   (map :transact/props)
                   (filter (fn [x]
                             (and
                              (implements? IVisualizable x)
                              (or
                               (not (implements? ITerritorialMixIn x))
                               (let [substage-id (get-substage-id x)]
                                 (= substage-id substage))))))
                   (take limit))] 
    [:div.chat-log
     (for [p plays] 
       (with-meta (visualizable/to-hiccup p ctx-ds {:viewpoint viewpoint}) {:key (action/get-id p)}))]))
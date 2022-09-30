(ns cocdan.fragment.chat-log 
  (:require [cocdan.data.action :as action]
            [cocdan.data.core :refer [get-substage-id ITerritorialMixIn]]
            [cocdan.data.visualizable :refer [IVisualizable to-hiccup]]
            [datascript.core :as d]))

(defn chat-log
  [{:keys [substage ctx-ds viewpoint limit] :or {limit 10}}]
  (let [plays (->> (d/datoms ctx-ds :avet :transaction/id)
                   reverse (map first)
                   (d/pull-many ctx-ds [:transaction/props])
                   (map :transaction/props)
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
       (with-meta (to-hiccup p ctx-ds {:viewpoint viewpoint}) {:key (action/get-id p)}))]))
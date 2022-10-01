(ns cocdan.fragment.chat-log
  (:require [cocdan.data.core :refer [get-tid]]
            [cocdan.data.territorial :refer [get-substage-id ITerritorialMixIn]]
            [cocdan.data.visualizable :as visualizable]
            [datascript.core :as d]))

(defn chat-log
  [{:keys [substage ctx-ds viewpoint limit] :or {limit 10}}]
  (let [plays (->> (d/datoms ctx-ds :avet :transaction/id)
                   reverse (map first)
                   (d/pull-many ctx-ds '[:transaction/props])
                   (map :transaction/props)
                   (filter (fn [x]
                             (and
                              (implements? visualizable/IVisualizable x)
                              (or
                               (not (implements? ITerritorialMixIn x))
                               (let [substage-id (get-substage-id x)]
                                 (= substage-id substage))))))
                   (take limit))]
    [:div.chat-log
     (for [p plays]
       (with-meta (visualizable/to-hiccup p ctx-ds {:viewpoint viewpoint}) {:key (get-tid p)}))]))

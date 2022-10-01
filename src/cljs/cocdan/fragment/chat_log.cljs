(ns cocdan.fragment.chat-log
  (:require [cocdan.aux :refer [remove-db-prefix]]
            [cocdan.data.territorial :refer [get-substage-id ITerritorialMixIn]]
            [cocdan.data.visualizable :as visualizable]
            [datascript.core :as d]
            [cocdan.database.ctx-db.core :as ctx-db]))

(defn chat-log
  [{:keys [substage ctx-ds viewpoint limit] :or {limit 10}}] 
  (let [transactions (->> (d/datoms ctx-ds :avet :transaction/id)
                          reverse (map first)
                          (d/pull-many ctx-ds '[*])
                          (map remove-db-prefix)
                          (filter (fn [{x :props}]
                                    (and
                                     (implements? visualizable/IVisualizable x)
                                     (or
                                      (not (implements? ITerritorialMixIn x))
                                      (let [substage-id (get-substage-id x)]
                                        (= substage-id substage))))))
                          (take limit))] 
    [:div.chat-log
     [:p.chat-anchor "-- end --"]
     (for [{:keys [ctx_id props id] :as transaction} transactions]
       (let [ctx (ctx-db/query-ds-ctx-by-id ctx-ds ctx_id)]
         (with-meta (visualizable/to-hiccup props ctx {:viewpoint viewpoint
                                                       :transaction transaction}) {:key id})))]))

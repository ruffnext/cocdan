(ns cocdan.fragment.chat-log.core
  (:require [cocdan.aux :refer [remove-db-prefix]]
            [cocdan.data.territorial :refer [get-substage-id ITerritorialMixIn]]
            [cocdan.data.visualizable :as visualizable]
            [cocdan.database.ctx-db.core :as ctx-db]
            [cocdan.fragment.chat-log.speak]
            [cocdan.fragment.chat-log.tpatch]
            [datascript.core :as d]
            [re-frame.core :as rf]))

(defonce chat-log-ui-cache (atom {}))

(defn chat-log
  [{:keys [substage stage-id viewpoint limit] :or {limit 10}}] 
  (let [ctx-ds @(ctx-db/query-stage-db stage-id)
        _refresh @(rf/subscribe [:partial-refresh/listen :chat-log])
        transactions (->> (d/datoms ctx-ds :avet :transaction/id)
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
                          (take limit))
        chat-log-ui-cache-deref @chat-log-ui-cache
        latest-ctx (atom nil)] 
    [:div.chat-log
     [:p.chat-anchor "-- end --"]
     (for [{:keys [ctx_id props id ack] :as transaction} transactions]
       (let [id-key (keyword (str id))
             cached-ui (id-key chat-log-ui-cache-deref)] 
         (if (and ack cached-ui)
           (with-meta cached-ui {:key id})
           (do
             (when-not (= (:context/id @latest-ctx) ctx_id)
               (reset! latest-ctx (ctx-db/query-ds-ctx-by-id ctx-ds ctx_id)))
             (let [res (with-meta (visualizable/to-hiccup props
                                                          @latest-ctx
                                                          {:viewpoint viewpoint
                                                           :transaction transaction}) {:key id})]
               (when ack (swap! chat-log-ui-cache #(assoc % id-key res)))
               res)))))]))

(ns cocdan.fragment.chat-log.core
  (:require [cocdan.aux :refer [remove-db-prefix]]
            [cocdan.data.mixin.territorial :refer [get-substage-id ITerritorialMixIn]]
            [cocdan.data.mixin.visualization :refer [IChatLogVisualization to-chat-log display?]]
            [cocdan.database.ctx-db.core :as ctx-db]
            [cocdan.fragment.chat-log.speak]
            [cocdan.fragment.chat-log.patch] 
            [cocdan.fragment.chat-log.dice]
            [cocdan.fragment.chat-log.narration]
            [datascript.core :as d]
            [re-frame.core :as rf]))

;; chat-log 的缓存，为了性能上的考量，不区分舞台，也不区分观察者
;; 因此需要注意对这个缓存的刷新
;; 格式为 {:transaction-id-A [hiccups] :transaction-id-B [hiccups]}
(defonce chat-log-ui-cache (atom {}))

(rf/reg-event-fx
 :chat-log/clear-cache!
 (fn [& _]
   (reset! chat-log-ui-cache {})))

(defn- query-can-visualizations
  [stage-ds substage limit]
  (->> (d/datoms stage-ds :avet :transaction/id)
       reverse (map first)
       (d/pull-many stage-ds '[*])
       (map remove-db-prefix)
       (filter (fn [{x :props}]
                 (and
                  (implements? IChatLogVisualization x)
                  (or
                   (not (implements? ITerritorialMixIn x))
                   (let [substage-id (get-substage-id x)]
                     (= substage-id substage)))
                  (display? x))))
       (take limit)))

(defn chat-log
  [{:keys [substage stage-id observer limit] :or {limit 10}}] 
  (let [stage-ds @(ctx-db/query-stage-db stage-id)
        _refresh @(rf/subscribe [:partial-refresh/listen :chat-log])
        transactions (query-can-visualizations stage-ds substage limit)
        chat-log-ui-cache-deref @chat-log-ui-cache  ; 提前解引用，提升性能
        latest-ctx (atom nil)] 
    [:div.chat-log
     [:p.chat-anchor "-- end --"]
     (if observer
       (for [{:keys [ctx_id props id ack] :as transaction} transactions]
         (let [id-key (keyword (str id))
               cached-ui (id-key chat-log-ui-cache-deref)]
           (if (and ack cached-ui)
             (with-meta cached-ui {:key id})
             (do
               (when-not (= (:context/id @latest-ctx) ctx_id)
                 (reset! latest-ctx (ctx-db/query-ds-ctx-by-id stage-ds ctx_id)))
               (let [res (with-meta (to-chat-log props
                                                 @latest-ctx
                                                 transaction
                                                 observer) {:key id})]
                 (when ack (swap! chat-log-ui-cache #(assoc % id-key res)))
                 res)))))
       [:p "您没有角色在这个舞台上"])]))

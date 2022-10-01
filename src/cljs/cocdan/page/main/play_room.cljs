(ns cocdan.page.main.play-room 
  (:require [cljs-http.client :as http]
            [clojure.core.async :refer [<! go]]
            [cocdan.core.ops.core :as op-core]
            [cocdan.core.play-room :as p-core] 
            [cocdan.database.ctx-db.core :as ctx-db]
            [cocdan.fragment.chat-log :as chat-log]
            [cocdan.fragment.input :as fragment-input]
            [cocdan.core.ws :as ws-core]
            [datascript.core :as d]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn- init-play-room
  [stage-id]
  (go
    (let [{:keys [status body]} (<! (http/get (str "/api/journal/s" stage-id)
                                              {:query-params {:with-context true}}))]
      (case status
        200 (let [{:keys [context transaction]} body
                  db (ctx-db/query-stage-db stage-id)
                  _ (ctx-db/import-stage-context! db context)
                  ds @db
                  latest-ctx (atom nil)
                  ds-records (reduce (fn [a {:keys [ctx_id] :as t}]
                                       (let [latest-ctx-val @latest-ctx
                                             latest-ctx-val (if (= (:context/id latest-ctx-val) ctx_id)
                                                              latest-ctx-val (reset! latest-ctx (ctx-db/query-ds-ctx-by-id ds ctx_id)))] 
                                         (concat a (op-core/ctx-generate-ds stage-id t latest-ctx-val))))
                                     [] transaction)]
              (d/transact! db (filter (fn [x] (not (contains? x :context/id))) ds-records))
              (rf/dispatch [:fx/refresh-stage-signal stage-id]))
        nil))))

(defn page
  []
  (r/with-let
    [stage-id (or @(rf/subscribe [:sub/stage-performing]) 1)
     substage-id (r/atom "lobby")
     avatar-id (r/atom nil)]
    (let [_refresh @(rf/subscribe [:play/refresh stage-id]) 
          ds (p-core/query-stage-ds stage-id) 
          latest-ctx (ctx-db/query-ds-latest-ctx ds)] 
      (if latest-ctx
        (let [channel @(rf/subscribe [:ws/channel stage-id])]
          (cond
            (= :unset channel) (do
                                 (js/console.log "初始化 WebSocket")
                                 (ws-core/init-ws! stage-id))
            (= :loading channel) (js/console.log "WebSocket 正在载入中")
            :else (js/console.log "WebSocket 已经载入完成"))
          [:div.container
           {:style {:padding-top "1em"
                    :padding-left "3em"
                    :padding-right "3em"}}
           [:p.has-text-centered @substage-id]
           [chat-log/chat-log
            {:ctx-ds ds
             :substage @substage-id
             :viewpoint @avatar-id}]
           [fragment-input/input
            {:context latest-ctx
             :substage @substage-id
             :hook-avatar-change (fn [x] (reset! avatar-id x))}]])
        (do
          (init-play-room stage-id)
          [:p "舞台尚未初始化"])))))

(ns cocdan.page.main.play-room 
  (:require [cljs-http.client :as http]
            [clojure.core.async :refer [<! go]]
            [cocdan.core.ops.core :as op-core]
            [cocdan.core.ws :as ws-core]
            [cocdan.data.mixin.territorial :refer [get-substage-id]]
            [cocdan.database.ctx-db.core :as ctx-db]
            [cocdan.fragment.avatar.indicator :as avatar-indicator]
            [cocdan.fragment.chat-log.core :as chat-log]
            [cocdan.fragment.chat-input.core :as fragment-input]
            [cocdan.fragment.substage.indicator :as substage-indicator]
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
              (rf/dispatch [:partial-refresh/refresh! :play-room]))
        nil))))

(defn page
  []
  (r/with-let
    [stage-id (or @(rf/subscribe [:sub/stage-performing]) 1)
     substage-id (r/atom "lobby")
     avatar-id (r/atom nil)]
    (let [_refresh @(rf/subscribe [:partial-refresh/listen :play-room])
          stage-db (ctx-db/query-stage-db stage-id)
          {{avatars :avatars} :context/props :as latest-ctx} (ctx-db/query-ds-latest-ctx @stage-db) 
          substage-id-deref @substage-id]
      (if latest-ctx
        (let [channel @(rf/subscribe [:ws/channel stage-id])] 
          (cond
            (= :unset channel) (do
                                 (js/console.log "初始化 WebSocket")
                                 (ws-core/init-ws! stage-id))
            (= :loading channel) (js/console.log "WebSocket 正在载入中")
            :else (js/console.log "WebSocket 已经完成初始化"))
          [:div.container
           {:style {:padding-top "1em"
                    :padding-left "3em"
                    :padding-right "3em"}}
           [:div.columns
            [:div.column.is-10
             [:p.has-text-centered "舞台"]
             [chat-log/chat-log
              {:stage-id stage-id
               :substage substage-id-deref
               :observer @avatar-id}]
             [fragment-input/input
              {:stage-id stage-id
               :context latest-ctx
               :substage @substage-id
               :hook-avatar-change (fn [x]
                                     (reset! avatar-id x)
                                     (when ((keyword (str x)) avatars)
                                       (reset! substage-id (get-substage-id ((keyword (str x)) avatars))))
                                     (reset! chat-log/chat-log-ui-cache {}))}]]
            [:div.column.is-2
             {:style {:font-size "12px"}}
             [:p "　"] ;; 空一行出来，与聊天框持平
             [substage-indicator/indicator {:stage-id stage-id
                                            :substage-id substage-id-deref
                                            :context latest-ctx
                                            :on-substage-change (fn [x]
                                                                  (reset! substage-id x)
                                                                  (reset! chat-log/chat-log-ui-cache {}))}]
             [avatar-indicator/indicator stage-id latest-ctx @avatar-id]]]])
        (do
          (init-play-room stage-id)
          [:p "舞台尚未初始化"])))))

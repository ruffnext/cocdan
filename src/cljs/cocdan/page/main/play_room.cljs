(ns cocdan.page.main.play-room
  (:require [cats.core :as m]
            [cljs-http.client :as http]
            [clojure.core.async :refer [<! go]]
            [cocdan.core.ops.core :as op-core]
            [cocdan.core.ws :as ws-core]
            [cocdan.data.stage :refer [new-stage]]
            [cocdan.database.ctx-db.core :as ctx-db]
            [cocdan.fragment.avatar.indicator :as avatar-indicator]
            [cocdan.fragment.chat-input.core :as fragment-input]
            [cocdan.fragment.chat-log.core :as chat-log]
            [cocdan.fragment.substage.indicator :as substage-indicator]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(rf/reg-event-fx
 :play/retrieve-logs
 (fn [_ [_ {:keys [stage-id desc? offset limit begin] :or {desc? true offset 0 limit 20 begin 0}}]]
   (go
     (let [{:keys [status body]} (<! (http/get (str "/api/journal/s" stage-id)
                                               {:query-params (-> {:with-context true
                                                                   :limit limit
                                                                   :desc desc?
                                                                   :offset offset
                                                                   :begin begin})}))]
       (case status
         200 (let [{:keys [context transaction]} body
                   stage-db (ctx-db/query-stage-db stage-id)
                   context-with-key (->> (map (fn [{:keys [id] :as ctx}] [(keyword (str id)) (update ctx :payload new-stage)]) context)
                                         (into {}))
                   transaction-oped (vec (for [{:keys [ctx_id] :as t} transaction]
                                           (let [ctx ((keyword (str ctx_id)) context-with-key)]
                                             (-> (op-core/ctx-run! t ctx)
                                                 (m/extract) first))))]
               (ctx-db/import-stage-context! stage-db context)
               (ctx-db/insert-transactions stage-db transaction-oped)
               
               (rf/dispatch [:partial-refresh/refresh! :play-room :chat-log]))
         nil)))
   {}))

(defn page
  []
  (r/with-let
    [stage-id (or @(rf/subscribe [:sub/stage-performing]) 1)]
    (let [_refresh @(rf/subscribe [:partial-refresh/listen :play-room])
          stage-db (ctx-db/query-stage-db stage-id)
          latest-ctx (ctx-db/query-latest-ctx stage-db)
          substage-id-deref @(rf/subscribe [:play-sub/substage-id])
          avatar-id-deref @(rf/subscribe [:play-sub/avatar-id])]
      (if latest-ctx
        (let [channel-status @(rf/subscribe [:ws/channel stage-id])]
          (cond
            (= :unset channel-status) (ws-core/init-ws! stage-id)
            (= :loading channel-status) (js/console.log "WebSocket 正在载入中")
            (= :failed channel-status) (js/console.log "WebSocket 载入失败")
            :else ())
          [:div.container
           {:style {:padding-top "1em"
                    :padding-left "3em"
                    :padding-right "3em"}}
           [:div.columns
            [:div.column.is-10
             [:p.has-text-centered "舞台"]
             [chat-log/auto-load-chat-log-view
              {:stage-id stage-id
               :can-show-more? true
               :substage-id substage-id-deref
               :observer avatar-id-deref}]
             [fragment-input/input
              {:stage-id stage-id
               :avatar-id avatar-id-deref
               :context latest-ctx
               :substage-id substage-id-deref}]]
            [:div.column.is-2
             {:style {:font-size "12px"}}
             [:p "　"] ;; 空一行出来，与聊天框持平
             [substage-indicator/indicator {:stage-id stage-id
                                            :substage-id substage-id-deref
                                            :context latest-ctx}]
             [avatar-indicator/indicator stage-id latest-ctx avatar-id-deref]]]])
        (do
          (rf/dispatch [:play/retrieve-logs {:stage-id stage-id}])
          [:p "舞台尚未初始化"])))))

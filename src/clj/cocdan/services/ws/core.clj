(ns cocdan.services.ws.core
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.tools.logging :as log]
            [cocdan.data.stage :refer [new-stage]]
            [cocdan.database.ctx-db.core :as ctx-db]
            [cocdan.db.monad-db :as monad-db]
            [cocdan.services.ws.aux :refer [snapshot-if-not-consistency!]]
            [cocdan.services.ws.db :as ws-db]
            [datascript.core :as d]
            [immutant.web.async :as async]))


(defonce current-channel (atom nil)) ; 测试用

(defn dispatch-transaction
  "一旦缓存数据库发生变动，则将该消息分发到对应的 channel 中"
  [channels report]
  (log/debug "dispatch transaction"))

(defn ds-persistence
  "数据库持久化的回调"
  [_]
  (log/debug "persistence"))


(defn connect! [channel]
  (let [res (m/mlet
             [{{user-id :identity} :session
               {stage-id :stage} :path-params :as _request} (either/right (async/originating-request channel)) ;; 解析 channel 为 request

              {stage-id :id :as stage} (monad-db/get-stage-by-id stage-id) ;; 查询舞台的信息

              avatars (monad-db/get-avatars-by-stage-id stage-id)  ;; 查询舞台上角色的信息

              stage-db (either/right (ctx-db/query-stage-db stage-id))]
             (ws-db/upsert! :channel {:ws channel
                                      :stage (:id stage)
                                      :user user-id})
             (d/listen! stage-db :dispatch-transaction (partial dispatch-transaction (ws-db/query-all-ws-by-stage-id stage-id))) ;; 更新分发回调
             (d/listen! stage-db :persistence ds-persistence)
            ;;  (when-not (query-is-stage-inited? @ws-db/db stage-id)
            ;;    (m/mlet [actions (stages-aux/query-actions-of-stage? stage-id)]
            ;;            (ws-db/upsert! :action actions)))
             (if (ctx-db/query-ds-latest-ctx @stage-db)
               (snapshot-if-not-consistency! stage-db (new-stage (assoc stage :avatars avatars)))  ;; 舞台已经初始化，检查一致性
               "从数据库中读出历史记录") ;; 舞台尚未初始化  
             (m/return {:stageId (str stage-id) :user user-id}))]
    (either/branch res
                   (fn [x]
                     (log/error x)
                     (async/close channel))
                   (fn [x]
                     (log/info "user " (:user x) " connected to stage " (:stageId x))))))

(defn disconnect! [channel {:keys [code reason]}]
  (log/info "close code:" code "reason:" reason)
  (let [eid (d/entid @ws-db/db [:channel/ws channel])]
    (when (not (nil? eid)) (d/transact! ws-db/db [[:db.fn/retractEntity eid]]))))

(defn on-message [channel msg-raw]
  (reset! current-channel channel)
  )

;"WebSocket callback functions"
(def websocket-callbacks 
  {:on-open connect!
   :on-close disconnect!
   :on-message on-message})


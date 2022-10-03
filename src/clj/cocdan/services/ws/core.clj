(ns cocdan.services.ws.core
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.tools.logging :as log]
            [cocdan.db.monad-db :as monad-db]
            [cocdan.services.ws.db :as ws-db]
            [datascript.core :as d]
            [immutant.web.async :as async]
            [cocdan.services.journal.core :as journal]
            [cocdan.aux :as data-aux]))


(defonce current-channel (atom nil)) ; 测试用

(defn dispatch-transaction
  "一旦缓存数据库发生变动，则将该消息分发到对应的 channel 中"
  [channels _register-key _stage-id transact _ctx]
  (doseq [channel channels]
    (async/send! channel (str (data-aux/remove-db-prefix transact)))))

(defn connect! [channel] 
  (either/branch
   (m/mlet
    [{{user-id :identity} :session
      {stage-id-str :stage} :path-params :as _request} (either/right (async/originating-request channel)) ;; 解析 channel 为 request
     stage-id (either/try-either (Integer/parseInt stage-id-str))
     _stage (monad-db/get-stage-by-id stage-id) ;; 查询舞台的信息
     _avatars (monad-db/get-avatars-by-stage-id stage-id)]
    (either/right
     {:stage stage-id :user user-id :ws channel}))

   (fn [left]
     (log/error left)
     (async/close channel))

   (fn [{:keys [stage] :as right}]
     (ws-db/upsert-one! :channel right)
     (journal/register-journal-hook stage :ws-transaction-dispatch
                                    (partial dispatch-transaction (ws-db/query-all-ws-by-stage-id stage))))))

(defn disconnect! [channel {:keys [code reason]}]
  (let [eid (d/entid @ws-db/db [:channel/ws channel])
        stage-id (:channel/stage (d/pull @ws-db/db '[:channel/stage] eid))]
    (when (not (nil? eid))
      (d/transact! ws-db/db [[:db.fn/retractEntity eid]])
      (journal/register-journal-hook
       stage-id :ws-transaction-dispatch
       (partial dispatch-transaction (ws-db/query-all-ws-by-stage-id stage-id))))
    (log/info "connection close code:" code "reason:" reason " stage:" stage-id)))

(defn on-message [channel _msg-raw]
  (reset! current-channel channel))

;"WebSocket callback functions"
(def websocket-callbacks 
  {:on-open connect!
   :on-close disconnect!
   :on-message on-message})

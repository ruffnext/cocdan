(ns cocdan.ws.core
  (:require
   [clojure.tools.logging :as log]
   [cocdan.users.core :refer [login?]]
   [cocdan.stages.auxiliary :as stages-aux]
   [cocdan.auxiliary :as gaux]
   [cocdan.shell.router :as router]
   [cocdan.ws.auxiliary :as ws-aux]
   [cocdan.ws.db :as ws-db]
   [cats.core :as m]
   [immutant.web.async :as async]
   [cats.monad.either :as either]
   [datascript.core :as d]
   [cocdan.avatars.auxiliary :as avatars-aux]))

(defonce current-channel (atom nil))

(defn connect! [channel]
  (let [res (m/mlet
             [request (either/right (async/originating-request channel))
              user (login? (:session request))
              stage (stages-aux/get-by-id? (:stage (:path-params request)))
              avatars (avatars-aux/list-avatars-by-stage? (:id stage))
              stageId (either/right (str (:id stage)))]
             (ws-db/upsert! ws-db/db :channel {:ws channel
                                               :stage-id (:id stage)
                                               :user user})
             (ws-db/upsert! ws-db/db :stage stage)
             (ws-db/upsert! ws-db/db :avatar avatars)
             (m/return {:stageId stageId :user user}))]
    (either/branch res
                   (fn [x]
                     (log/error x)
                     (async/close channel))
                   (fn [x]
                     (log/info "user " (:id (:user x)) " connected to stage " (:stageId x))))))

(defn disconnect! [channel {:keys [code reason]}]
  (log/info "close code:" code "reason:" reason)
  (let [eid (d/entid @ws-db/db [:channel/ws channel])]
    (when (not (nil? eid)) (d/transact! ws-db/db [[:db.fn/retractEntity eid]]))))

(defn handle-msg-response
  "do async send"
  [res channel]
  (let [{forward :forward :as msg}
        (either/branch res
                       (fn [x] (if (map? x)
                                 x
                                 (assoc (ws-aux/make-msg 0 "alert" (str x))
                                        :forward false)))
                       (fn [x] x))
        responseMsg (gaux/->json (dissoc msg :forward))]
    (log/debug msg)
    (if forward
      (doseq [channel (ws-db/query-channels-by-channel @ws-db/db channel)]
        (async/send! channel responseMsg))
      (async/send! channel responseMsg))))

(defn on-message-ng! [channel msg-raw]
  (reset! current-channel channel)
  (-> (m/mlet [{avatar-id :avatar :as msg-json} (ws-aux/parse-message msg-raw)
               {stage-id :stage-id user :user} (ws-db/pull-channel @ws-db/db channel)
               _check (-> (m/do-let
                           (ws-aux/check-avatar-access stage-id avatar-id (:id user))))
               response (router/handle-msg msg-json channel)]
              (m/return response))
      (handle-msg-response channel)))

(comment
  ; test send msg
  (let [channels (->> (d/q '[:find ?channel
                             :where [?eid :channel/ws ?channel]]
                           @ws-db/db)
                      (reduce into []))]
    (handle-msg-response (-> (ws-aux/make-msg 0 "msg" "Hello")
                             either/right) (first channels)))
  )

(def websocket-callbacks
  "WebSocket callback functions"
  {:on-open connect!
   :on-close disconnect!
   :on-message on-message-ng!})

(defn ws-handler [request]
  (async/as-channel request websocket-callbacks))

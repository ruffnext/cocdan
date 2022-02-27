(ns cocdan.ws.core
  (:require
   [clojure.tools.logging :as log]
   [cocdan.users.core :refer [login?]]
   [cocdan.stages.auxiliary :as stages-aux]
   [cocdan.avatars.auxiliary :as avatars-aux]
   [cocdan.shell.router :as router]
   [cocdan.ws.auxiliary :as ws-aux]
   [cocdan.shell.db :as s-db]
   [cocdan.ws.db :as ws-db :refer [remove-db-perfix query-is-stage-inited?]]
   [cats.core :as m]
   [immutant.web.async :as async]
   [cats.monad.either :as either]
   [datascript.core :as d]
   [cocdan.auxiliary :as gaux]
   [clojure.core.async :refer [go]]))

(defonce current-channel (atom nil))

(defn connect! [channel]
  (let [res (m/mlet
             [request (either/right (async/originating-request channel))
              {user-id :id} (login? (:session request))
              {stage-id :id :as stage} (stages-aux/get-by-id? (:stage (:path-params request)))
              avatars (avatars-aux/list-avatars-by-stage? (:id stage))]
             (ws-db/upsert! ws-db/db :channel {:ws channel
                                               :stage (:id stage)
                                               :user user-id})
             (when-not (query-is-stage-inited? @ws-db/db stage-id)
               (m/mlet [actions (stages-aux/query-actions-of-stage? stage-id)]
                       (ws-db/upsert! s-db/db :action actions)))
             (when-not (s-db/query-latest-ctx-eid stage-id)
               (s-db/reset-stage-actions! stage-id)
               (stages-aux/reset-stage-actions! stage-id))
             (ws-db/upsert! ws-db/db :stage stage)
             (ws-db/upsert! ws-db/db :avatar avatars)
             (let [current-order (s-db/query-max-order-of-stage-action stage-id)]
               (if current-order
                 (async/send! channel  (->> current-order
                                            (s-db/query-stage-action? stage-id)
                                            remove-db-perfix
                                            gaux/->json))
                 (go 
                   (s-db/make-snapshot! stage avatars))))
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
  (either/branch-left
   (m/mlet [{avatar-id :avatar :as msg-json} (ws-aux/parse-message msg-raw)
            {stage-id :stage user-id :user} (ws-db/pull-channel @ws-db/db channel)
            user-avatars (avatars-aux/list-avatars-by-user? user-id)
            _check (m/do-let
                    (ws-aux/check-avatar-access avatar-id user-id user-avatars))
            response (router/handle-msg (assoc msg-json
                                               :time (.getTime (java.util.Date.))
                                               :stage stage-id) channel)]
           (s-db/action! stage-id (:type response) response (:time response))
           (m/return ""))
   (fn [x]
     (log/error x)
     (async/send! channel (if (map? x)
                            (gaux/->json x)
                            (gaux/->json (ws-aux/make-msg 0 "alert" (str x))))))))

(def websocket-callbacks
  "WebSocket callback functions"
  {:on-open connect!
   :on-close disconnect!
   :on-message on-message})

(defn ws-handler [request]
  (m/mlet [_ (login? (:session request))]
          (either/right (async/as-channel request websocket-callbacks))))
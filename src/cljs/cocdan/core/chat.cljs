(ns cocdan.core.chat
  (:require
   [re-frame.core :as rf]
   [cats.core :as m]
   [cats.monad.either :as either]
   [cocdan.auxiliary :refer [handle-keys <-json <-json swap-filter-list-map! ->json ->json]]
   [re-posh.core :as rp]
   [datascript.core :as d]
   [cocdan.core.stage :refer [posh-stage-by-id]]
   [cocdan.core.avatar :refer [posh-my-avatars]]
   [cocdan.core.log :refer [append-action! register-action-to-log-listener]]
   [cocdan.db :as gdb]
   [clojure.core.async :refer [timeout <! go]]
   [reagent.core :as r]))

(defn make-msg
  ([avatar type msg]
   {:time (. js/Date now)
    :avatar avatar
    :type type
    :msg msg})
  ([avatar type msg time]
   {:time time
    :avatar avatar
    :type type
    :msg msg}))

(defn make-speak-loudly-msg
  [avatar msg]
  {:time (. js/Date now)
   :avatar avatar
   :type "speak-loudly"
   :msg msg})

(defn make-speak-normal-msg
  [avatar msg]
  {:time (. js/Date now)
   :avatar avatar
   :type "speak-normal"
   :msg msg})

(defn make-speak-whisper-msg
  [avatar to-avatar-ids msg]
  {:time (. js/Date now)
   :avatar avatar
   :type "speak-whisper"
   :receiver to-avatar-ids
   :msg msg})

(defn make-action-use
  [avatar-id items-to-use action-describe]
  {:time (. js/Date now)
   :avatar avatar-id
   :type "use-items"
   :use items-to-use
   :msg action-describe})

(defn make-system-msg
  [msg]
  {:time (. js/Date now)
   :avatar 0
   :type "system-msg"
   :msg msg})

(goog-define ws-host "localhost")
(goog-define ws-port 3000)
(register-action-to-log-listener gdb/db)

(defn init-stage-ws!
  [{stage-id :stage-id}]
  (let [url (str "ws://"  ws-host ":" ws-port "/ws/" stage-id)]
    (m/mlet [channel (either/try-either (js/WebSocket. url))]
            (do
              (set! (.-onmessage channel) #(rf/dispatch [:event/chat-on-message stage-id %]))
              (set! (.-onclose channel) #(rf/dispatch [:event/chat-on-close stage-id %]))
              (set! (.-onerror channel) #(js/console.log %))
              (rp/dispatch [:rpevent/upsert :stage {:id stage-id
                                                    :channel channel}]))
            (either/right "success"))))

(defn check-msg-syntax
  [raw-msg]
  (m/foldm
   (fn [_ x]
     (if (not (nil? (x raw-msg)))
       (either/right raw-msg)
       (either/left (str "invalid message received! missing field " x)))) (either/right "") [:stage :type :fact :time :order]))

(defn- on-message!
  [db [_query-id _ msg]]
  (either/branch-left
   (m/mlet [raw-msg (m/->=
                     (either/try-either (<-json (.-data msg)))
                     check-msg-syntax)]
           (append-action! gdb/db raw-msg)
           (m/return ""))
   (fn [x]
     (js/console.log (str x))))
  db)

(comment
  (append-action! gdb/db {:order 1 :time 2})
  )

(defonce reconnect-retry-remain (r/atom {}))

(defn- reconnect
  [stage-id on-stage-avatars]
  (let [remain ((keyword (str stage-id)) @reconnect-retry-remain)]
    (cond
      (nil? remain) (do
                      (swap! reconnect-retry-remain #(assoc % (keyword (str stage-id)) 3))
                      (reconnect stage-id on-stage-avatars))
      (pos-int? remain) (go
                          (<! (timeout 1000))
                          (swap! reconnect-retry-remain #(assoc % (keyword stage-id) (- remain 1)))
                          (init-stage-ws! {:stage-id stage-id}))
      :else ""
      ;; (append-msg stage-id
      ;;             (-> (for [avatar on-stage-avatars]
      ;;                   (assoc (make-system-msg "Reconnection failed after 3 retry") :receiver (:id avatar)))
      ;;                 vec))
      )))

(defn- on-close
  [db [_query-id stage-id event]]
  (let [my-avatars (->> @(posh-my-avatars gdb/db)
                        (gdb/pull-eids gdb/db))
        on-stage-avatars (filter #(= (:on_stage %) stage-id) my-avatars)
        stage-eid (first @(posh-stage-by-id gdb/db stage-id))
        stage (gdb/pull-eid gdb/db stage-eid)]
    (case (.-code event)
      1005 (reconnect stage-id on-stage-avatars)
      1006 "reject reconnection"
      :else (js/console.log (str "cannot handle ws close code " (.-code event))))
    (when stage-eid
      (d/transact! gdb/db [[:db.fn/retractEntity stage-eid]])
      (d/transact! gdb/db [(assoc (handle-keys :stage (dissoc stage :channel)) :db/add -1)]))

    (assoc db :stages (swap-filter-list-map!
                       (:stages db)
                       #(= (:id %) stage-id)
                       (fn [stage]
                         (assoc stage :channel nil))))))

(defn- send-message
  [_app-data [_driven-by stage-id msg]]
  (let [channel (:channel (->> @(posh-stage-by-id gdb/db stage-id)
                                     (gdb/pull-eid gdb/db)))]
    (if (nil? channel)
      (js/console.log (str "stage " stage-id "'s channel is nil!"))
      (.send channel (->json msg)))
    {}))

(doseq [[event f] {:event/chat-on-close on-close
                   :event/chat-on-message on-message!}]
  (rf/reg-event-db event f))

(doseq [[event f] {:event/chat-send-message send-message}]
  (rf/reg-event-fx event f))

(doseq [[fx f] {:fx/chat-new-stage init-stage-ws!}]
  (rf/reg-fx fx f))

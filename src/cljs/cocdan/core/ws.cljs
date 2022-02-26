(ns cocdan.core.ws
  (:require
   [cats.core :as m]
   [re-frame.core :as rf]
   [cats.monad.either :as either]
   [datascript.core :as d]
   [cocdan.core.stage :refer [posh-stage-by-id]]
   [cocdan.db :as gdb]
   [cocdan.auxiliary :refer [->json handle-keys <-json]]
   [cocdan.core.log :refer [append-action!]]))

(goog-define ws-host "localhost")
(goog-define ws-port 3000)

(defn init-ws!
  [_ [_ stage-id retry-remain]]
  (let [url (str "ws://"  ws-host ":" ws-port "/ws/" stage-id)
        retry-remain (if (nil? retry-remain)
                       (atom 3)
                       retry-remain)]
    (either/branch-right
     (either/try-either (js/WebSocket. url))
     (fn [channel]
       (set! (.-onopen channel) #(rf/dispatch [:ws-event/on-open stage-id % retry-remain]))
       (set! (.-onmessage channel) #(rf/dispatch [:ws-event/on-message stage-id %]))
       (set! (.-onclose channel) #(rf/dispatch [:ws-event/on-close stage-id % retry-remain]))
       (set! (.-onerror channel) #(js/console.log %))
       (rf/dispatch [:rpevent/upsert :stage {:id stage-id
                                             :channel channel}])))
    {}))

(defn- check-msg-syntax
  [raw-msg]
  (m/foldm
   (fn [_ x]
     (if (not (nil? (x raw-msg)))
       (either/right raw-msg)
       (either/left (str "invalid message received! missing field " x))))
   (either/right raw-msg) [:stage :type :fact :time :order]))

(defn- on-message
  [_ [_ _stage-id event]]
  (either/branch-right
   (m/->=
    (either/try-either (<-json (.-data event)))
    check-msg-syntax)
   (fn [msg]
     (append-action! gdb/db msg)))
  {})

(defn- on-close
  [_ [_ stage-id event retry-remain]]
  (let [stage-eid (d/entid @gdb/db [:stage/id stage-id])
        stage (gdb/pull-eid gdb/db stage-eid)
        close-code (.-code event)]
    (swap! retry-remain (fn [x] (- x 1)))
    (cond
      (= 0 @retry-remain) "failed to reconnect after 3 retries"
      (= 1000 close-code) "gracefully close"
      (= 1005 close-code) (rf/dispatch [:ws-event/init! stage-id retry-remain])
      (= 1006 close-code) "reject reconnection"
      :else (js/console.log (str "cannot handle ws close code " (.-code event))))
    (when stage-eid ; remove channel of stage
      (d/transact! gdb/db [[:db.fn/retractEntity stage-eid]])
      (d/transact! gdb/db [(assoc (handle-keys :stage (dissoc stage :channel)) :db/add -1)]))
    {}))

(defn- send-msg
  [_ [_ stage-id msg]]
  (when-let [channel (->> @(posh-stage-by-id gdb/db stage-id)
                          (gdb/pull-eid gdb/db)
                          :channel)]
    (.send channel (->json msg))) {})

(defn- on-open
  [_ [_ _stage-id _event retry-remain]]
  (reset! retry-remain 3)
  {})

(doseq [[k event-handle] {:ws-event/init! init-ws!
                          :ws-event/on-message on-message
                          :ws-event/on-close on-close
                          :ws-event/on-open on-open
                          :ws-event/send-msg send-msg}]
  (rf/reg-event-fx k event-handle))
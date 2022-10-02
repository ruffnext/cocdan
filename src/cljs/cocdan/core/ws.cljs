(ns cocdan.core.ws
  (:require
   [re-frame.core :as rf]
   [cats.monad.either :as either]
   [cocdan.aux :as data-aux]))

(goog-define ws-host "localhost")
(goog-define ws-port 3001)

(rf/reg-sub
 :ws/channel
 (fn [db [_ stage-id]]
   (or (get-in db [:ws (keyword (str stage-id)) :channel]) :unset)))

(rf/reg-event-db
 :ws/change-channel!
 (fn [db [_ stage-id channel]]
   (assoc-in db [:ws (keyword (str stage-id))] {:channel channel
                                                :retry-remain 3})))

(defn- on-open
  [stage-id event]
  (rf/dispatch [:ws/change-channel! stage-id :loaded])
  (js/console.log "OPENED"))

(defn- on-message
  [stage-id event]
  (let [transaction (data-aux/<-json (.-data event))] 
    (rf/dispatch [:play/execute stage-id [transaction]])))

(defn- on-close
  [stage-id event]
  (let [retry-remain (atom 3)
        close-code (.-code event)
        reconnect-status (cond
                           (= 0 @retry-remain) "failed to reconnect after 3 retries"
                           (= 1000 close-code) "gracefully close"
                           (= 1005 close-code) "need reconnect"
                           (= 1006 close-code) "server reject ws connect"
                           :else (js/console.log (str "cannot handle ws close code " (.-code event))))] 
    (rf/dispatch-sync [:ws/change-channel! stage-id :failed])))

(defn init-ws!
  [stage-id]
  (let [url (str "ws://"  ws-host ":" ws-port "/ws/" stage-id)]
    (rf/dispatch-sync [:ws/change-channel! stage-id :loading])
    (either/branch-right
     (either/try-either (js/WebSocket. url))
     (fn [channel]
       (set! (.-onopen channel) (partial on-open stage-id))
       (set! (.-onmessage channel) (partial on-message stage-id))
       (set! (.-onclose channel) (partial on-close stage-id))
       (set! (.-onerror channel) #(js/console.log %))))))

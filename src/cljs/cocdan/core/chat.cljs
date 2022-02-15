(ns cocdan.core.chat
  (:require
   [re-frame.core :as rf]
   [cats.core :as m]
   [cats.monad.either :as either]
   [cocdan.auxiliary :as gaux]
   [re-posh.core :as rp]
   [cocdan.core.indexeddb :as idb]
   [cocdan.db :as gdb]))

(def alert "alert")
(def sync "sync")
(def info  "info")
(def msg  "msg")

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

(defn- make-stage-ws
  [{stage-id :stage-id}]
  (let [url (str "ws://localhost:3000/ws/" stage-id)]
    (m/mlet [channel (either/try-either (js/WebSocket. url))]
            (do
              (set! (.-onmessage channel) #(rf/dispatch [:event/chat-on-message stage-id %]))
              (set! (.-onclose channel) #(rf/dispatch [:event/chat-on-close stage-id %]))
              (rp/dispatch [:rpevent/upsert :stage {:id stage-id
                                                    :channel channel}])))))

(defn- append-msg
  [stage-id msg]
  (let [msg' (cond
               (or (list? msg)
                   (vector? msg)
                   (seq? msg)) (->> (reduce (fn [a x] (conj a (if (not (nil? x))
                                                                (assoc x :mainstage stage-id)
                                                                nil))) [] msg)
                                    (filter #(not (nil? %))))
               :else msg)]
    (when (not (nil? msg'))
      (js/console.log msg')
      (rp/dispatch [:rpevent/upsert :message msg'])
      (idb/append-message @idb/idb msg'))))

(defn- parse-message
  [msg' _stage-id]
  (let [my-avatars (gdb/posh-my-avatars gdb/conn)
        res (m/mlet [{_avatar-id :avatar-id
                      msg :msg
                      msg-type :type
                      msg-time :time 
                      substage :substage :as raw-msg} (either/try-either (gaux/<-json (.-data msg')))
                     _ (m/foldm (fn [_ x] (if (not (nil? (x raw-msg)))
                                            (either/right "")
                                            (either/left (str "invalid message received! missing field " x)))) (either/right "") [:avatar :msg :type :time])]
                    (m/return (case msg-type
                                "sync" (let [{target :target value :value} msg]
                                         (rp/dispatch [:rpevent/upsert (keyword target) value])
                                         (assoc (make-msg 0 "debug-info" (str "Sync called for " target) msg-time)
                                                :value value))
                                "msg" (-> (for [{{avatar-substage :substage} :attributes avatar-id :id} my-avatars]
                                            (when (= avatar-substage substage)
                                              (assoc raw-msg :receiver avatar-id)))
                                          vec)
                                "system-msg" (-> (for [{{avatar-substage :substage} :attributes avatar-id :id} my-avatars]
                                                   (when (= avatar-substage substage)
                                                     (assoc raw-msg :receiver avatar-id)))
                                                 vec)
                                "pm" raw-msg
                                raw-msg)))]
    (either/branch res
                   (fn [x]
                     (either/left (make-msg 0 alert x)))
                   (fn [x] 
                     (either/right x)))))

(defn- on-message
  [db [_query-id stage-id _msg]]
  (let [lobby-msg' (m/mlet [raw-msg (parse-message _msg stage-id)]
                           (m/return raw-msg))
        lobby-msg (either/branch lobby-msg'
                                 (fn [x] x)
                                 (fn [x] x))]
    (append-msg stage-id lobby-msg)
    db))

(defn- on-close
  [db [_query-id stage-id _event]]
  (assoc db :stages (gaux/swap-filter-list-map!
                     (:stages db)
                     #(= (:id %) stage-id)
                     (fn [stage]
                       (assoc stage :channel nil))))
  (append-msg stage-id (make-msg 0 "alert" "Connection Closed"))
  (js/console.log _event))

(defn- send-message
  [_app-data [_driven-by stage-id msg]]
  (let [channel (:stage/channel @(rp/subscribe [:rpsub/stage stage-id]))]
    (if (nil? channel)
      (js/console.log (str "stage " stage-id "'s channel is nil!"))
      (.send channel (gaux/->json msg)))))

(defn- sub-substage-msgs
  [db [_query-id stage-id substage-id]]
  (->> db
       :stages
       (filter #(= (:id %) stage-id))
       first
       :sub-stages
       (filter #(= (:id %) substage-id))
       first
       :history-msg))

(doseq [[event f] {:event/chat-on-close on-close
                 :event/chat-on-message on-message}]
  (rf/reg-event-db event f))

(doseq [[event f] {:event/chat-send-message send-message}]
  (rf/reg-event-fx event f))

(doseq [[fx f] {:fx/chat-new-stage make-stage-ws}]
  (rf/reg-fx fx f))

(doseq [[sub f] {:subs/chat-substage-msgs sub-substage-msgs}]
  (rf/reg-sub sub f))
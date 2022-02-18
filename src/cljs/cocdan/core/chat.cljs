(ns cocdan.core.chat
  (:require
   [re-frame.core :as rf]
   [cats.core :as m]
   [cats.monad.either :as either]
   [cocdan.auxiliary :as gaux]
   [re-posh.core :as rp]
   [cocdan.core.indexeddb :as idb]
   [cocdan.db :as gdb]
   [clojure.string :as str]
   [clojure.core.async :refer [go <! timeout]]))

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

(defn- make-stage-ws
  [{stage-id :stage-id}]
  (let [url (str "ws://"  ws-host ":" ws-port "/ws/" stage-id)]
    (m/mlet [channel (either/try-either (js/WebSocket. url))]
            (do
              (set! (.-onmessage channel) #(rf/dispatch [:event/chat-on-message stage-id %]))
              (set! (.-onclose channel) #(rf/dispatch [:event/chat-on-close stage-id %]))
              (rp/dispatch [:rpevent/upsert :stage {:id stage-id
                                                    :channel channel}]))
            (either/right "success"))))

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
      (rp/dispatch [:rpevent/upsert :message msg'])
      (idb/append-message @idb/idb msg'))))

(defn- get-hear-difficulty
  [{{v-from :声音} :coc} {{v-to :声音} :coc}]
  (let [vfrom (if (pos-int? v-from)
                v-from
                50)
        vto (if (pos-int? v-to)
              v-to
              50)]
    (+ vfrom vto)))

(comment
  (flatten (conj [{:a "b"} {:c "d"} []] [{:a "bbb"}]))
  )

(defn- handle-speak
  [{msg-type :type
    substage :substage
    msg-text :msg
    speaker-id :avatar :as raw-msg} my-avatars i-have-control? stage]
  (let [substages (-> stage :attributes :substages)
        speaker-substage ((keyword substage) substages)
        my-same-stage-avatars (->> (filter #(= (-> % :attributes :substage) substage) my-avatars)
                                   (map :id))
        my-aside-stage-avatars (let [speakers-substage ((keyword substage) substages)
                                     conntented-substage (set (-> speakers-substage
                                                                  :coc
                                                                  :连通区域))]
                                 (filter #(contains? conntented-substage (-> % :attributes :substage)) my-avatars))
        loud-hear-avatar-maps (-> (for [audience my-aside-stage-avatars]
                                    (let [audience-substage ((keyword (-> audience :attributes :substage)) substages)
                                          diff (get-hear-difficulty speaker-substage audience-substage)
                                          dice (+ (rand-int 100) 1)]
                                      (cond
                                        (< diff dice) [(:id audience) (:name speaker-substage) "can hear"]
                                        (< (/ diff 2) dice) [(:id audience) (:name speaker-substage) "barely hear"]
                                        :else [(:id audience) (:name speaker-substage) "cant hear"])))
                                  vec)
        normal-receive-avatar-ids (set (->> (conj my-same-stage-avatars (if i-have-control?
                                                                          i-have-control?
                                                                          []))
                                            flatten))
        whisper-receivers (:receiver raw-msg)
        normal-reveice-msg (for [aid normal-receive-avatar-ids]
                             (assoc raw-msg :receiver aid))]
    (-> (case msg-type
          "speak-loudly" (conj
                          (for [[aid substage-name status] loud-hear-avatar-maps]
                            (let [avatar (gdb/query-avatar-by-id @gdb/conn speaker-id)]
                              (when avatar
                                (case status
                                  "can hear" (assoc (make-system-msg (str "你听见从" substage-name "传来" (:name avatar) "的声音：" msg-text))
                                                    :receiver aid)
                                  "barely hear" (assoc (make-system-msg (str "你听见从" substage-name "传来说话声，但是你听不清"))
                                                       :receiver aid)
                                  []))))
                          normal-reveice-msg)
          "speak-normal" normal-reveice-msg
          "speak-whisper" (for [aid (conj whisper-receivers speaker-id)]
                            (merge (assoc raw-msg :receiver aid)
                                   (if (= (:owned_by stage) speaker-id)
                                     {:type "system-msg"}
                                     {})))
          [])
        flatten
        vec)))

(defn- dispatch-messages
  [{sender :avatar
    msg :msg
    msg-type :type
    msg-time :time
    substage :substage :as raw-msg} my-avatars i-have-control? stage]
  (cond
    (str/starts-with? msg-type "speak")
    (handle-speak raw-msg my-avatars i-have-control? stage)
    
    (= "use-items" msg-type)
    (-> (for [{{avatar-substage :substage} :attributes avatar-id :id} my-avatars]
          (when (= avatar-substage substage)
            (let [items (:use raw-msg)
                  avatar (gdb/query-avatar-by-id @gdb/conn sender)]
              (assoc raw-msg
                     :receiver avatar-id
                     :type "system-msg"
                     :msg (str (:name avatar) "使用" (str/join "," items) "：" (:msg raw-msg))))))
        vec)

    (= "sync" msg-type)
    (let [{target :target value :value} msg]
      (rp/dispatch [:rpevent/upsert (keyword target) value])
      (assoc (make-msg 0 "debug-info" (str "Sync called for " target) msg-time)
             :value value))

    (= "system-msg" msg-type)
    (-> (for [{{avatar-substage :substage} :attributes avatar-id :id} my-avatars]
          (when (= avatar-substage substage)
            (assoc raw-msg :receiver avatar-id)))
        vec)

    :else
    raw-msg))

(defn- parse-message
  [msg' stage-id]
  (let [my-avatars (gdb/posh-my-avatars gdb/conn)
        res (m/mlet [raw-msg (either/try-either (gaux/<-json (.-data msg')))

                     _ (m/foldm (fn [_ x] (if (not (nil? (x raw-msg)))
                                            (either/right "")
                                            (either/left (str "invalid message received! missing field " x)))) (either/right "") [:avatar :msg :type :time])
                     stage (either/try-either (gdb/posh-stage-by-id gdb/conn stage-id))]
                    (m/return (dispatch-messages raw-msg my-avatars (:id (gdb/posh-i-have-control? gdb/conn stage-id)) stage)))]
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
  (let [my-avatars (gdb/query-my-avatars @gdb/conn)
        on-stage-avatars (filter #(= (:on_stage %) stage-id) my-avatars)]
    (go
      (-> (make-stage-ws {:stage-id stage-id})
          (either/branch-left ; second retry
           #(do
              (<! (timeout 1000))
              (make-stage-ws {:stage-id stage-id})))
          (either/branch-left ; third retry
           #(do
              (<! (timeout 1000))
              (make-stage-ws {:stage-id stage-id})))
          (either/branch
           #(append-msg stage-id (-> (for [avatar on-stage-avatars]
                                       (assoc (make-system-msg "Reconnection failed after 3 retry") :receiver (:id avatar)))
                                     vec))
           #())))
    (assoc db :stages (gaux/swap-filter-list-map!
                       (:stages db)
                       #(= (:id %) stage-id)
                       (fn [stage]
                         (assoc stage :channel nil))))))

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

(comment
  (either/branch-left (either/right "hello")
                      (fn [x] x)))
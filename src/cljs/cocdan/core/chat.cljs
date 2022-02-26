(ns cocdan.core.chat)

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

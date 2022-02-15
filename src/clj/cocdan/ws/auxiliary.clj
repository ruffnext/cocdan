(ns cocdan.ws.auxiliary
  (:require
   [cats.monad.either :as either]
   [cats.core :as m]
   [cocdan.auxiliary :as gaux]
   [cocdan.avatars.auxiliary :as avatarsaux]))

(defn
  wrap-basic-check
  [res response]
  (either/branch res
                 (fn [x] (assoc response
                                :forward false
                                :msg x
                                :avatar 0))
                 (fn [_] response)))

(def
  msg-fields
  [:avatar :msg :type :time])

(defn make-msg
  ([avatar type msg time]
   {:avatar avatar
    :type type
    :msg msg
    :time time})
  ([avatar type msg]
   (make-msg avatar type msg (.getTime (java.util.Date.)))))

(defn parse-message
  [msg-raw]
  (let [res (m/mlet [msg-parsed (either/try-either (gaux/<-json msg-raw))
                     _ (m/foldm (fn [_ x] (if (not (nil? (x msg-parsed)))
                                            (either/right "")
                                            (either/left (str "invalid message received! missing field " x)))) (either/right "") msg-fields)]
                    (m/return msg-parsed))]
    (either/branch res
                   (fn [x]
                     (either/left (make-msg 0 "alert" x)))
                   (fn [x]
                     (either/right x)))))

(defn check-avatar-access
  [stage-id avatar-id user-id]
  (m/mlet [avatars (avatarsaux/list-avatars-by-stage? stage-id)
           user-controllable (either/right (set (reduce (fn [a x]
                                                          (if (= (:controlled_by x) user-id)
                                                            (conj a (:id x))
                                                            a)) [] avatars)))
           _ (if (empty? user-controllable)
               (either/left "you have no avatar available in this stage")
               (either/right ""))
           _ (if (contains? user-controllable avatar-id)
               (either/right "")
               (either/left (format "you have no permission to use avatar %s" (str avatar-id))))]
          (m/return "")))
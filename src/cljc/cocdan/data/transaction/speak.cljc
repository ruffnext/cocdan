(ns cocdan.data.transaction.speak 
  (:require [cocdan.data.mixin.territorial :refer [get-substage-id ITerritorialMixIn]]))

(defrecord Speak [avatar substage message props] 
  ITerritorialMixIn
  (get-substage-id [_this] substage))

(defn new-speak
  [avatar-id substage-id message props]
  (Speak. avatar-id substage-id message props))

(defn handler-speak
  [{ctx :context/props} {{:keys [avatar message props]} :props}]
  (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])
        avatar-substage (if (satisfies? ITerritorialMixIn avatar-record)
                          (get-substage-id avatar-record)
                          nil)]
    (new-speak  avatar avatar-substage message props)))

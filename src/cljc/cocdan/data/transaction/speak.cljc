(ns cocdan.data.transaction.speak 
  (:require [cocdan.data.mixin.territorial :refer [get-substage-id ITerritorialMixIn]]))

(defrecord Speak [avatar substage message props] 
  ITerritorialMixIn
  (get-substage-id [_this] substage))

(defn new-speak
  [avatar-id substage-id message props]
  (Speak. avatar-id substage-id message props))

(defn handler-speak
  [{ctx :context/props} {{:keys [avatar substage message props]} :props}]
  (let [substage-id (if substage substage
                        (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])]
                          (if (satisfies? ITerritorialMixIn avatar-record)
                            (get-substage-id avatar-record)
                            nil)))]
    (new-speak  avatar substage-id message props)))

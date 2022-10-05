(ns cocdan.data.transaction.speak 
  (:require [cocdan.data.mixin.territorial :refer [get-substage-id ITerritorialMixIn]]
            [cats.monad.either :as either]))

(defrecord Speak [avatar substage message props] 
  ITerritorialMixIn
  (get-substage-id [_this] substage))

(defn new-speak
  [avatar-id substage-id message props]
  (Speak. avatar-id substage-id message props))

(defn handler-speak-transaction
  [{ctx :context/props} {{:keys [avatar substage message props]} :props}]
  (let [substage-id (if substage substage
                        (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])]
                          (if (satisfies? ITerritorialMixIn avatar-record)
                            (get-substage-id avatar-record)
                            nil)))]
    (either/right (new-speak  avatar substage-id message props))))

(defrecord Narration [substage message props]
  ITerritorialMixIn
  (get-substage-id [_this] substage))

(defn handle-narration-transaction
  [_ctx {{:keys [substage message props]} :props :as _transaction}]
  (either/right (->Narration substage message props)))
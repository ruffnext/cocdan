(ns cocdan.data.action 
  (:require [cocdan.core.ops.core :refer [register-transaction-handler]]
            [cocdan.data.core :refer [ITransaction]]
            [cocdan.data.territorial :refer [get-substage-id ITerritorialMixIn]]))

"动作类型，大部分数据都是这个类型"

(defrecord Speak [id time ctx_id avatar substage message props]
  ITransaction
  (get-tid [_this] id)
  (get-time [_this] time)
  (get-ctx_id [_this] ctx_id)

  ITerritorialMixIn
  (get-substage-id [_this] substage))

(defn new-speak
  [id time ctx_id avatar-id substage-id message props]
  (Speak. id time ctx_id avatar-id substage-id message props))

(defn handler-speak
  [{ctx_id :context/id ctx :context/props} {:keys [id time props]}]
  (let [{:keys [avatar message props]} props]
    (new-speak id time ctx_id avatar (get-substage-id (get-in ctx [:avatars (keyword (str avatar))])) message props)))

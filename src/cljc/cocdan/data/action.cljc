(ns cocdan.data.action 
  (:require [cocdan.database.ctx-db.core :refer [query-ds-ctx-by-id]]
            [cocdan.data.core :refer [get-substage-id to-ds
                                      ITerritorialMixIn
                                      IDsRecord]]
            [datascript.core :as d]))

(defprotocol IAction
  (get-id [this] "返回该动作的索引")
  (get-time [this] "返回动作执行的时间") 
  (get-type [this] "返回该动作的类型（小写字符串）")
  (get-ctx [this ds] "返回该动作执行时的上下文"))

(defrecord Speak [id time ctx-id avatar substage message props]
  IAction
  (get-type [_this] "speak")
  (get-id [_this] id)
  (get-time [_this] time)
  (get-ctx [_this ds] (:context/props (d/pull ds '[:context/props] [:context/id ctx-id])))

  ITerritorialMixIn
  (get-substage-id [_this] substage)

  IDsRecord
  (to-ds [this]
    {:transaction/id id
     :transaction/type (get-type this)
     :transaction/props this}))

(defn new-speak
  [id time ctx-id avatar-id substage-id {:keys [message props]}]
  (Speak. id time ctx-id avatar-id substage-id message props))

(defn new-play
  "play 指的是那些不造成状态变化的行为，或者说 transact/type 不等于 :transact 的类型"
  [id ctx-id ctx time {:keys [type avatar payload]}]
  (let [res (case type 
              :speak [(new-speak id time ctx-id avatar (get-substage-id (get-in ctx [:avatars (keyword (str avatar))])) payload)]
              [])]
    (vec (map to-ds res))))
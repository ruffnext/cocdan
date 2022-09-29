(ns cocdan.data.action 
  (:require [cocdan.core.aux :refer [query-ctx]]
            [cocdan.data.core :refer [get-substage-id ITerritorialMixIn]]
            [datascript.core :as d]))

(defprotocol IAction
  (get-id [this] "返回该动作的索引")
  (get-time [this] "返回动作执行的时间") 
  (get-type [this] "返回该动作的类型")
  (get-ctx [this ds] "返回该动作执行时的上下文"))

(defrecord Speak [id time ctx-id avatar substage message props]
  IAction
  (get-type [_this] :speak)
  (get-id [_this] id)
  (get-time [_this] time) 
  (get-ctx [_this ds] (:context/props (d/pull ds '[:context/props] [:context/id ctx-id])))
  
  ITerritorialMixIn
  (get-substage-id [_this] substage))

(defn new-speak
  [id time ctx-id avatar-id substage-id {:keys [message props]}]
  (Speak. id time ctx-id avatar-id substage-id message props))

(defn new-play
  "play 指的是那些不造成状态变化的行为，或者说 transact/type 不等于 :transact 的类型"
  [id ctx-id ds time {:keys [type avatar payload]}]
  (let [res (case type
              :no-ctx-needed []
              (let [ctx (query-ctx ds ctx-id)
                    avatar-record (get-in ctx [:avatars (keyword (str avatar))])] ;; 这部分类型需要取得当前的上下文后才能解析 
                (case type
                  :speak [(new-speak id time ctx-id avatar (get-substage-id avatar-record) payload)]
                  [])))]
    (vec (map (fn [x] {:transact/id id
                       :transact/type (get-type x)
                       :transact/props x}) res))))
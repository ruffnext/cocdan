(ns cocdan.data.partial-refresh 
  (:require [re-frame.core :as rf]))

; 对于部分 Transaction 来说，很有可能只需要刷新 UI 的一部分内容
; 例如聊天信息只刷新 chat-log，而别的地方不需要刷新
; 如果全部都刷新的话将会导致 render 过于频繁
; 函数 refresh-key 返回一个 keyword ，一旦收到这类消息时，
; 系统将去刷新这部分的组件

(defprotocol IPartialRefresh
  (refresh-key [this]))

(rf/reg-sub
 :partial-refresh/listen
 (fn [db [_ listen-key]]
   (or (get-in db [:partial-refresh listen-key]) 0)))

(rf/reg-event-fx
 :partial-refresh/refresh!
 (fn [{:keys [db]} [_ listen-key]] 
   {:db (update-in db [:partial-refresh listen-key] #(if % (inc %) 1))}))

(rf/reg-event-fx
 :partial-refresh/reset!
 (fn [{:keys [db]} [_ listen-key]]
   {:db (assoc-in db [:partial-refresh listen-key] 0)}))

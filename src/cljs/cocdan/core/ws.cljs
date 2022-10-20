(ns cocdan.core.ws
  (:require [clojure.core.async :refer [<! go timeout]]
            [clojure.edn :refer [read-string]]
            [re-frame.core :as rf]))

(def max-retries 3)
(defonce retry-remain (atom 3))
(declare init-ws!)

(rf/reg-sub
 :ws/channel
 (fn [db [_ stage-id]]
   (or (get-in db [:ws (keyword (str stage-id)) :channel]) :unset)))

(rf/reg-event-db
 :ws/change-channel!
 (fn [db [_ stage-id channel]]
   (assoc-in db [:ws (keyword (str stage-id))] {:channel channel})))

(defn- on-open
  [stage-id _event]
  (rf/dispatch [:ws/change-channel! stage-id :loaded])
  (rf/dispatch [:ui/toast "info" "" "与服务器建立链接成功"])
  (reset! retry-remain 3))

(defn- on-message
  [stage-id event]
  (let [transaction (read-string (.-data event))] 
    (rf/dispatch [:play/execute-many-from-remote stage-id [transaction]])))

(defn- on-close
  [stage-id event]
  (let [close-code (.-code event)
        [info-type info-title info-content]
        (cond
          (= 0 @retry-remain) ["error" "无法与服务器建立连接" (str "尝试与服务器建立链接已失败 " max-retries " 次，放弃重连")]
          (= 1000 close-code) ["info" "关闭与服务器的连接" "服务器请求断开链接。\n链接已被断开"]
          (= 1005 close-code) ["error" "与服务器失去链接" "正在尝试重连"]
          (= 1006 close-code) ["error" "服务器拒绝链接" "无法与服务器建立链接，服务器拒绝了客户端的链接请求！"]
          :else ["error" "致命错误" (str "cannot handle ws close code " (.-code event))])] 
    (rf/dispatch [:ws/change-channel! stage-id :failed])
    (rf/dispatch [:ui/toast info-type info-title info-content])
    (when (pos-int? @retry-remain)
      (swap! retry-remain dec)
      (go
        (<! (timeout 3000))
        (init-ws! stage-id)))))

(defn init-ws!
  [stage-id]
  (let [protocol (case (-> js/window .-location .-protocol) "http:" "ws" "wss")
        url (str protocol "://"  (-> js/window .-location .-host) "/ws/" stage-id)]
    (rf/dispatch-sync [:ws/change-channel! stage-id :loading])
    (when-let [channel (js/WebSocket. url)]
      (set! (.-onopen channel) (partial on-open stage-id))
      (set! (.-onmessage channel) (partial on-message stage-id))
      (set! (.-onclose channel) (partial on-close stage-id))
      (set! (.-onerror channel) #(js/console.log %)))))

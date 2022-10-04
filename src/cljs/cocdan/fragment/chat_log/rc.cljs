(ns cocdan.fragment.chat-log.rc 
  (:require [cocdan.data.mixin.visualization :refer [IChatLogVisualization]]
            [cocdan.data.transaction.dice :refer [RC]]))

(extend-type RC
  IChatLogVisualization
  (to-chat-log [{:keys [avatar attr attr-val dice-result]} {ctx :context/props} {ack :ack} _observer] 
    (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])]
      [:div
       [:div.is-flex
        [:p.transact-key (str (name avatar-record) "进行检定投掷「" attr "/ " attr-val "」 : ")]
        [:p.transact-value (if ack (str dice-result " " (if (>= attr-val dice-result) "成功" "失败")) "等待骰子响应")]]]))
  (display? [_this] true))

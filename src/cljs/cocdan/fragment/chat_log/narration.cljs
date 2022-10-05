(ns cocdan.fragment.chat-log.narration
  (:require [cocdan.data.mixin.visualization :refer [IChatLogVisualization]]
            [cocdan.data.transaction.speak :as speak]))

(extend-type speak/Narration
  IChatLogVisualization
  (to-chat-log [{:keys [message] :as _this} _ctx _transaction _observer]
    [:p.chat-log-narration message])
  (display? [_this] true))

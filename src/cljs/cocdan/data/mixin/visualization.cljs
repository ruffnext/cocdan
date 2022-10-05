(ns cocdan.data.mixin.visualization)

(defprotocol IChatLogVisualization
  (to-chat-log [this ctx transaction observer] "返回一个 hiccup")
  (display? [this] "返回该 chat-log 是否应当被显示。例如有些 log 只有 KP 才显示"))

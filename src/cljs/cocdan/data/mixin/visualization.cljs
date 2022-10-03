(ns cocdan.data.mixin.visualization)

(defprotocol IChatLogVisualization
  (to-chat-log [this ctx transaction observer])
  (display? [this]))

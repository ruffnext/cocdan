(ns cocdan.data.visualizable)

(defprotocol IVisualizable
  (to-hiccup [this ctx kwargs]))

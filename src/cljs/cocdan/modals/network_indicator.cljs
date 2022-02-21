(ns cocdan.modals.network-indicator)

(defn network-indicator
  [channel]
  (when (nil? channel)
    [:div.network-indicator.has-text-danger.sketch.has-text-centered
     "网络连接丢失"]))
(ns cocdan.modals.network-indicator 
  (:require [cats.monad.either :as either]))

(defn network-indicator
  [channel]
  (let [status (cond
                 (nil? channel) "尚未连接"
                 (either/left? channel) (either/branch-left channel (fn [x] (str x)))
                 (either/right? channel) nil)]
    (when status
      [:div.network-indicator.has-text-danger.sketch.has-text-centered
       status])))
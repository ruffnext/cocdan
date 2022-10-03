(ns cocdan.fragment.chat-log.patch
  (:require [cocdan.data.transaction.patch :refer [TPatch]]
            [cocdan.data.mixin.visualization :refer [IChatLogVisualization]]
            [cocdan.core.settings :as settings]))

(extend-type TPatch
  IChatLogVisualization
  (to-chat-log [{:keys [ops]} _ctx _transaction _observer] 
    [:div
     (map-indexed (fn [i [k _before after]]
                    (with-meta [:div.is-flex
                                [:p.transact-key (str k " : ")]
                                [:p.transact-value (str after)]] {:key i})) ops)])
  (display? [_this] (settings/query-setting-value-by-key :is-kp)))

(ns cocdan.fragment.chat-log.st 
  (:require [cocdan.core.settings :as settings]
            [cocdan.data.mixin.visualization :refer [IChatLogVisualization]]
            [cocdan.data.transaction.dice :refer [ST]]))

(extend-type ST
  IChatLogVisualization
  (to-chat-log [{:keys [avatar attr-map]} _ctx _transaction _observer]
    [:div
     (map-indexed (fn [i [k after]]
                    (with-meta [:div.is-flex
                                [:p.transact-key (str avatar "进行属性设置"  k " : ")]
                                [:p.transact-value (str after)]] {:key i})) attr-map)])
  (display? [_this] (settings/query-setting-value-by-key :is-kp)))

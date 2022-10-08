(ns cocdan.fragment.chat-log.patch
  (:require [cocdan.data.transaction.patch :refer [TPatch]]
            [cocdan.data.mixin.visualization :refer [IChatLogVisualization]]))

(extend-type TPatch
  IChatLogVisualization
  (to-chat-log
    [{:keys [ops]} {stage :context/props} _transaction observer]
    (let [[_substage-before substage-after] (->> (map (fn [[a b c]]
                                                        (let [re-result (= (str "avatars." observer ".substage") (name a))]
                                                          (when re-result [b c]))) ops)
                                                 (filter some?) first)
          hints (if substage-after
                  (let [{:keys [name description]} (get-in stage [:substages (keyword substage-after)])]
                    [[:div.is-flex
                      [:p.transact-key (str "你来到了 「" name "」")]
                      [:p.transact-value {:style {:font-style "italic"}}
                       description]]])
                  [])
          kv-changes (if (= 0 observer)
                       [(map-indexed (fn [i [k _before after]]
                                       (with-meta [:div.is-flex
                                                   [:p.transact-key (str k " : ")]
                                                   [:p.transact-value (str after)]] {:key i})) ops)]
                       [])]
      (vec (concat [:div]
                   kv-changes
                   hints))))
  (display? [_this] true))

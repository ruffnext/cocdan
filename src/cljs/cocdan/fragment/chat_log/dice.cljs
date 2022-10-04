(ns cocdan.fragment.chat-log.dice
  (:require [cocdan.data.mixin.visualization :refer [IChatLogVisualization]]
            [cocdan.data.transaction.dice :as dice]
            [cocdan.core.coc.attrs :as attrs-core]))

(defn- visualize-dice
  [{:keys [avatar attr attr-val dice-result] :as this} ctx]
  (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])
        attr-zh-name (attrs-core/get-attr-localization-name attr :zh)]
    [:div
     [:div.is-flex
      [:p.dice-hint (str (name avatar-record) " 正在进行检定投掷「" attr-zh-name " / " attr-val "」 : ")]
      [:p.dice-value
       (str (case (dice/get-is-success this)
              :dice/big-failure "大失败！"
              :dice/big-success "大成功！"
              :dice/difficult-success "困难成功！"
              :dice/failure "失败"
              :dice/success "成功"
              :dice/unknown "异常？！"
              :dice/very-difficult-success "极难成功！"
              :dice/waiting-ack "等待骰子确认......")
            "（" dice-result "）")]]]))

(extend-type dice/RC
  IChatLogVisualization
  (to-chat-log [this {ctx :context/props} _transaction _observer]
    (visualize-dice this ctx))
  (display? [_this] true))

(extend-type dice/RA
  IChatLogVisualization
  (to-chat-log [this {ctx :context/props} _transaction _observer]
    (visualize-dice this ctx))
  (display? [_this] true))

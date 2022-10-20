(ns cocdan.fragment.chat-log.dice
  (:require [cocdan.core.coc.attrs :as attrs-core]
            [cocdan.core.settings :refer [translate]]
            [cocdan.data.mixin.visualization :refer [IChatLogVisualization]]
            [cocdan.data.transaction.dice :as dice :refer [get-is-success]]))

(defn- visualize-dice-hint
  [avatar-name hint-name dice-type dice-result attr-val success-level]
  [:p.dice-hint (str avatar-name " 正在进行 " hint-name " ： " dice-type "=" (or dice-result "??") "/" attr-val " " (translate success-level))])

(defn- visualize-dice
  [{:keys [avatar attr attr-val dice-result] :as this} ctx]
  (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])
        attr-zh-name (attrs-core/get-attr-localization-name (keyword attr) :zh)]
    [:div
     [:div.is-flex
      (visualize-dice-hint (name avatar-record) (str attr-zh-name " 检定") "1D100" dice-result attr-val (dice/get-success-level this))
      [:p.dice-value
       ""]]]))

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

(extend-type dice/SC
  IChatLogVisualization
  (to-chat-log
    [{:keys [avatar san-loss attr-val loss-on-failure loss-on-success dice-result] :as this} {ctx :context/props} _transaction _observer]
    (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])
          success-level (dice/get-success-level this)]
      [:div
       [:div.is-flex
        (visualize-dice-hint (name avatar-record) "San Check" "1D100" dice-result attr-val success-level)
        (if dice-result
          [:p.dice-value
           (translate :sc/result {:san-loss (str (if (get-is-success success-level) loss-on-success loss-on-failure) "=" san-loss)
                                  :san-remain (max (- attr-val san-loss) 0)})]
          [:p ""])]]))
  (display? [_this] true))

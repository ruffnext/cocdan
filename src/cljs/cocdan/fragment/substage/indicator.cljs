(ns cocdan.fragment.substage.indicator
  (:require ["antd" :refer [Avatar Divider Input Modal Dropdown]]
            [cocdan.core.settings :as settings]
            [cocdan.data.mixin.territorial :refer [get-substage-id]]
            [cocdan.data.performer.core :as performer] 
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn indicator
  [stage-id ctx substage-id]
  (r/with-let
    [substage-editor-open? (r/atom false)
     substage-clicked (r/atom nil)]
    (let [{:keys [substages avatars]} (:context/props ctx)
          {description :description
           substage-name :name} ((keyword substage-id) substages)
          avatars (map second avatars)
          same-substage-avatars (filter #(= substage-id (get-substage-id %)) avatars)]
      [:div.substage-indicator-container
       [:> Divider (r/as-element [:select
                                  {:style {:border "0" :appearance "none"}
                                   :value substage-id
                                   :on-change #(js/console.log (-> % .-target .-value))}
                                  (map (fn [k] ^{:key k} [:option (name k)]) (conj (vec (keys substages)) "+"))])]
       (if (settings/query-setting-value-by-key :is-kp)
         [:> (.-TextArea Input)
          {:autoSize true
           :bordered false
           :defaultValue (or description "请输入当前场景的介绍")
           :onBlur (fn [x]
                     (let [value (-> x .-target .-value)]
                       (when-not (= description value)
                         (rf/dispatch [:play/execute-transaction-props-easy!
                                       stage-id "update"
                                       [[(keyword (str "substages." substage-id ".description")) description value]]]))))
           :style {:font-size "12px"
                   :padding-left "0px"
                   :padding-right "0px"}}]
         [:p description])
       [:> (.-Group Avatar)
        {:maxCount 2
         :maxStyle {:color "#f56a00"  :backgroundColor "#fde3cf"}}
        (map (fn [avatar]
               (with-meta
                 [:> Avatar
                  {:src (performer/get-header avatar :default)
                   :title (str (name avatar) "\n" (performer/get-description avatar))}]
                 {:key (:id avatar)})) same-substage-avatars)]
       [:> Modal
        {}]])))

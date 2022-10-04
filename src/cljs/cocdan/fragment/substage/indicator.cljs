(ns cocdan.fragment.substage.indicator
  (:require ["antd" :refer [Avatar Divider Input]]
            [cocdan.core.settings :as settings]
            [cocdan.data.mixin.territorial :refer [get-substage-id]]
            [cocdan.data.performer.core :as performer]
            [cocdan.modal.substage-edit :as substage-edit]
            [cocdan.modal.avatar-edit :as avatar-edit]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn- substage-selector
  [{:keys [substage-id substages on-change]}]
  (let [all-options (conj (vec (map (fn [[k {substage-name :name}]] [(name k) substage-name]) substages)) ["" "新建子舞台"])] 
    [:select
     {:style {:border "0" :appearance "none" :text-align "center"}
      :value substage-id
      :on-change #(on-change (-> % .-target .-value))}
     (map (fn [[value display-name]] ^{:key value} [:option {:value value} display-name]) all-options)]))

(defn- substage-description-editor
  [{stage-id :stage-id
    {description :description
     substage-id :id} :substage}]
  (if (settings/query-setting-value-by-key :is-kp)
    (with-meta   ;; force re-render this react component
      [:> (.-TextArea Input)
       {:autoSize true
        :bordered false
        :defaultValue description
        :placeholder "请输入子舞台介绍"
        :onBlur (fn [x]
                  (let [value (-> x .-target .-value)]
                    (when-not (= description value)
                      (rf/dispatch [:play/execute-transaction-props-easy!
                                    stage-id "update"
                                    [[(keyword (str "substages." substage-id ".description")) description value]]]))))
        :style {:font-size "12px"
                :padding-left "0px"
                :padding-right "0px"}}]
      {:key (js/Math.random)})
    [:p description]))

(defn- substage-avatars-indicator
  [{:keys [substage-id stage]}]
  (let [avatars (map second (:avatars stage))
        same-substage-avatars (filter #(and (= substage-id (get-substage-id %)) (not= (:id %) 0)) avatars)]
    (if (seq same-substage-avatars)
      [:> (.-Group Avatar)
       {:maxCount 2
        :maxStyle {:color "#f56a00"  :backgroundColor "#fde3cf"}}
       (map (fn [avatar]
              (with-meta
                [:> Avatar
                 {:src (performer/get-header avatar :default)
                  :title (str (name avatar) "\n" (performer/get-description avatar))
                  :on-click (fn [_] (avatar-edit/launch {:stage stage :avatar-id (:id avatar)}))}]
                {:key (:id avatar)})) same-substage-avatars)]
      [:p "这儿没有任何角色"])))

(defn indicator
  [{stage-id :stage-id
    substage-id :substage-id
    ctx :context
    hook-substage-change :on-substage-change}]
  (let [{:keys [substages] :as stage} (:context/props ctx)
        on-substage-change (fn [x]
                             (if (empty? x)
                               (substage-edit/launch {:stage stage :substage-id nil})
                               (hook-substage-change x)))]
    [:div.substage-indicator-container
     {:onDoubleClick (fn [_]
                       (substage-edit/launch {:stage stage
                                              :substage-id substage-id}))}
     [:> Divider
      (r/as-element [substage-selector {:substage-id substage-id
                                        :substages substages
                                        :on-change on-substage-change}])]
     [substage-description-editor {:stage-id stage-id
                                   :substage ((keyword substage-id) substages)}]
     [substage-avatars-indicator {:substage-id substage-id
                                  :stage (:context/props ctx)}]]))

(ns cocdan.fragment.chat-input.core
  (:require ["antd" :refer [Cascader]] 
            [cocdan.data.mixin.territorial :refer [get-substage-id]] 
            [cocdan.fragment.chat-input.dice :as dice-input]
            [cocdan.fragment.chat-input.input :as chat-input]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [cocdan.core.settings :as settings]))

(defn input
  [{{{stage-id :id :as stage-ctx} :context/props} :context
    substage-id :substage
    hook-avatar-change :hook-avatar-change}]
  (r/with-let [action-value (r/atom nil)
               avatar-id (r/atom nil)
               user-id @(rf/subscribe [:common/user-id])] 
    (let [_refresh @(rf/subscribe [:partial-refresh/listen :chat-input])
          all-avatars (map second (:avatars stage-ctx))
          is-kp (settings/query-setting-value-by-key :is-kp)
          same-substage-avatars (filter #(or (= (get-substage-id %) substage-id) (and is-kp (= 0 (:id %)))) all-avatars)
          controllable-avatars (->> (filter (fn [{:keys [controlled_by]}]
                                              (or is-kp (= controlled_by user-id)))
                                            same-substage-avatars)
                                    (map (fn [{:keys [id name]}] [id name]))) 
          on-cascader-change (fn [v _] (reset! action-value v))
          on-avatar-change (fn [[v] _]
                             (reset! avatar-id v)
                             (when hook-avatar-change (hook-avatar-change v)))
          
          _ (when (nil? @avatar-id) (on-avatar-change [(-> controllable-avatars first first)] nil))] 
      (if @avatar-id
        [:div
         [:> Cascader
          {:options (map (fn [[id name]] {:value id :label name}) controllable-avatars)
           :value [@avatar-id]
           :allowClear false
           :onChange on-avatar-change}]

         [:> Cascader
          {:options [{:value "speak" :label "说话"}
                     {:value "move" :label "移动"}
                     {:value "dice" :label "检定"
                      :children [{:value "attr"
                                  :label "属性检定"
                                  :children [{:value "str" :label "力量"}
                                             {:value "mov" :label "移动"}]}
                                 {:value "skill"
                                  :label "技能检定"
                                  :children [{:value "shoot" :label "射击"}]}]}]
           :allowClear false
           :value @action-value
           :onChange on-cascader-change}]
         (case (first @action-value)
           "speak"
           [chat-input/input {:stage-id stage-id
                              :substage-id substage-id
                              :avatar-id @avatar-id
                              :avatars (:avatars stage-ctx)}]
           "dice"
           [dice-input/dice {:stage-id stage-id
                             :avatar-id @avatar-id
                             :attr (last @action-value)}]

           [:p "请选择行动"])]
        [:p "在这个舞台上你没有可操作的角色"]))))

;; (rf/dispatch [:play/execute-transaction-props-easy! 1 "st" {:avatar 0 :attr-map {:str 99}} true])

(ns cocdan.fragment.input
  (:require ["antd" :refer [Cascader Mentions]]
            [cocdan.core.ops.core :refer [make-transaction]] 
            [cocdan.data.territorial :refer [get-substage-id]] 
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn input
  [{{{stage-id :id :as stage-ctx} :context/props} :context
    substage-id :substage
    hook-avatar-change :hook-avatar-change}]
  (r/with-let [input-value (r/atom "")
               action-value (r/atom nil)
               avatar-id (r/atom nil)
               is-clear (r/atom false)
               user-id @(rf/subscribe [:common/user-id])]
    (let [all-avatars (map second (:avatars stage-ctx))
          same-substage-avatars (filter #(= (get-substage-id %) substage-id) all-avatars)
          controllable-avatars (->> (filter (fn [{:keys [controlled_by]}]
                                              (= controlled_by user-id))
                                            same-substage-avatars)
                                    (map (fn [{:keys [id name]}] [id name])))
          mentionable-avatars (filter #(not= (:id %) @avatar-id) same-substage-avatars)
          on-cascader-change (fn [v _] (reset! action-value v))
          on-avatar-change (fn [[v] _]
                             (reset! avatar-id v)
                             (when hook-avatar-change (hook-avatar-change v)))
          on-textarea-enter (fn [x]
                              (let [value (-> x .-target .-value) 
                                    this-op (make-transaction nil nil 4 "speak" {:avatar @avatar-id :message value :props {}})]
                                (rf/dispatch [:play/execute-one-remotly! stage-id this-op])
                                (reset! input-value "")
                                (reset! is-clear true)))
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
         [:> Mentions
          {:autoSize true
           :spellCheck false
           :disabled (if (and (seq @action-value) (some? @avatar-id)) false true)
           :value @input-value
           :onChange (fn [e] (if @is-clear
                               (swap! is-clear not)
                               (reset! input-value e)))
           :onPressEnter on-textarea-enter}
          (doall
           (for [a mentionable-avatars]
             (with-meta [:> (.-Option Mentions) {:value (str (:id a))} (:name a)] {:key (:id a)})))]
         ]
        [:p "在这个舞台上你没有可操作的角色"]))))
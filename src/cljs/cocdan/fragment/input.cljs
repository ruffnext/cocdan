(ns cocdan.fragment.input
  (:require ["antd" :refer [Cascader Mentions]]
            [cocdan.core.aux :refer [query-latest-ctx-id]]
            [cocdan.core.ops :refer [make-op OP-PLAY]]
            [cocdan.core.play-room :refer [query-stage-db]]
            [cocdan.data.core :refer [get-substage-id]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn input
  [{{stage-id :id :as stage-ctx} :context substage-id :substage hook-avatar-change :hook-avatar-change}]
  (r/with-let [input-value (r/atom "")
               action-value (r/atom nil)
               avatar-id (r/atom nil)
               is-clear (r/atom false)
               user-id @(rf/subscribe [:common/user-id])]
    (let [last-transact-id @(rf/subscribe [:play/latest-transact-id stage-id])
          all-avatars (map second (:avatars stage-ctx))
          same-substage-avatars (filter #(= (get-substage-id %) substage-id) all-avatars)
          controllable-avatars (->> (filter (fn [{:keys [controlled-by]}]
                                              (= controlled-by user-id))
                                         same-substage-avatars)
                                 (map (fn [{:keys [id name]}] [id name])))
          mentionable-avatars (filter #(not= (:id %) @avatar-id) same-substage-avatars)
          on-cascader-change (fn [v _] (reset! action-value v))
          on-avatar-change (fn [[v] _]
                             (reset! avatar-id v)
                             (when hook-avatar-change (hook-avatar-change v)))
          on-textarea-enter (fn [x]
                              (let [value (-> x .-target .-value)
                                    last-ctx-id (query-latest-ctx-id (query-stage-db stage-id))
                                    next-transact-id (inc last-transact-id)
                                    this-op (make-op next-transact-id last-ctx-id 4 OP-PLAY {:type :speak :avatar @avatar-id :payload {:message value :props {}}})]
                                (rf/dispatch [:play/execute stage-id [this-op]])
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
             (with-meta [:> (.-Option Mentions) {:value (:id a)} (:name a)] {:key (:id a)})))]]
        [:p "在这个舞台上你没有可操作的角色"]))))
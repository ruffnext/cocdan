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
    substage-id :substage-id
    avatar-id :avatar-id}]
  (r/with-let [action-value (r/atom ["input" "speak"]) 
               user-id @(rf/subscribe [:common/user-id])] 
    (let [_refresh @(rf/subscribe [:partial-refresh/listen :chat-input])
          all-avatars-list (map second (:avatars stage-ctx))
          is-kp (settings/query-setting-value-by-key :game-play/is-kp)
          same-substage-avatars (filter #(or (= (get-substage-id %) substage-id) (and is-kp (= 0 (:id %)))) all-avatars-list)
          same-substage-avatars-id (map :id same-substage-avatars)
          controllable-avatars (->> (filter (fn [{:keys [controlled_by] :as avatar}]
                                              (or is-kp  ;; 如果是 KP，则能控制所有的角色 
                                                  (and (= controlled_by user-id)
                                                       (= (get-substage-id avatar) substage-id))))
                                            all-avatars-list))
          on-cascader-change (fn [v _] (reset! action-value v))
          on-avatar-change (fn [[v] _]
                             (rf/dispatch [:play/change-avatar-id! v])
                             (when-not (and is-kp (= 0 v))  ;; 当切换到 kp 时，不切换到角色所在的子舞台上
                               (let [avatars (:avatars stage-ctx)]
                                 (when ((keyword (str v)) avatars) 
                                   (rf/dispatch [:play/change-substage-id! (get-substage-id ((keyword (str v)) avatars))])))))]
      (when-not (contains? (set same-substage-avatars-id) avatar-id)
        (cond
          (seq controllable-avatars) (on-avatar-change [(-> controllable-avatars first :id)] nil)
          (some? avatar-id) (on-avatar-change nil nil)
          :else ()))
      (if avatar-id
        [:div
         [:> Cascader
          {:options (map (fn [{:keys [id name]}] {:value id :label name}) controllable-avatars)
           :value [avatar-id]
           :allowClear false
           :onChange on-avatar-change}]

         [:> Cascader
          {:options [(if is-kp
                       {:value "input" :label "说话"
                        :children [{:value "speak" :label "通常"}
                                   {:value "narration" :label "旁白"}]}
                       {:value "input" :label "说话"
                        :children [{:value "speak" :label "通常"}]})
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
           "input"
           [chat-input/input {:stage-id stage-id
                              :substage-id substage-id
                              :avatar-id avatar-id
                              :avatars (:avatars stage-ctx)
                              :speak-type (second @action-value)}]
           "dice"
           [dice-input/dice {:stage-id stage-id
                             :avatar-id avatar-id
                             :attr (last @action-value)}]

           [:p "请选择行动"])]
        [:p "在这个舞台上你没有可操作的角色"]))))

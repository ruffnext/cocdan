(ns cocdan.components.chatting-input
  (:require
   [cocdan.core.chat :as chat]
   [reagent.core :as r]
   ["react-select/creatable" :refer (default) :rename {default react-select}]
   [re-posh.core :as rp]
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defn- gen-voice-options
  [substage-avatars avatar-id-use]
  (let [avatars (filter #(not= (:id %) avatar-id-use) substage-avatars)]
    (for [{id :id name :name} avatars]
      {:value id :label name})))

(defn- gen-item-options
  [items]
  (let [items-name (->> (reduce (fn [a [_k xs]]
                                  (conj a (map :name xs))) [] items)
                        (reduce into []))]
    (for [name items-name]
      {:value name :label name})))

(comment
  (let [items {:头部 [{:name "帽子"}] :腰部 [{:name "手枪"}]}]
    (->> (reduce (fn [a [_k xs]]
                   (conj a (map :name xs))) [] items)
         (reduce into []))))

(defn chatting-input
  [stage-id substage-avatars avatar-avilables {avatar-id-use :id {{items :items} :coc} :attributes}]
  (when (not (nil? avatar-id-use))
    (r/with-let [last-avatar-use (r/atom "")
                 text (r/atom "")
                 action (r/atom "说话")
                 voice (r/atom "正常")
                 voice-to-target (r/atom [])
                 items-to-use (r/atom [])
                 send-msg (fn [id]
                            (when (not (str/blank? @text))
                              (let [msg (case @action
                                          "说话" (case @voice
                                                 "大声" (chat/make-speak-loudly-msg id @text)
                                                 "正常" (chat/make-speak-normal-msg id @text)
                                                 "小声" (chat/make-speak-whisper-msg id (reduce (fn [a x]
                                                                                                (conj a (:value x))) [] @voice-to-target) @text))
                                          "使用" (chat/make-action-use id (reduce (fn [a x]
                                                                                  (conj a (:value x))) [] @items-to-use) @text)
                                          "msg")]
                                (rf/dispatch [:event/chat-send-message stage-id msg]))))
                 reset-input (fn []
                               (reset! voice-to-target [])
                               (reset! last-avatar-use avatar-id-use)
                               (reset! items-to-use []))
                 on-select-avatar-change #(rp/dispatch [:rpevent/upsert :stage {:id stage-id
                                                                                :current-use-avatar (js/parseInt (-> % .-target .-value))}])]
      (when (not= @last-avatar-use avatar-id-use) (reset-input))
      (let [voice-options (gen-voice-options substage-avatars avatar-id-use)
            item-options (gen-item-options items)]
        [:div
         [:div {:class "field has-addons"}
          [:p.control>a.button.is-static "角色"]
          [:p.control
           {:style {:margin-right "1em"}}
           [:span.select
            [:select
             {:title "选择操作的角色"
              :on-change on-select-avatar-change
              :value avatar-id-use}
             (for [avatar avatar-avilables]
               ^{:key (str "cias-" (:id avatar))} [:option {:value (:id avatar)} (str (:name avatar))])]]]
          [:p.control
           {:style {:margin-left "1em"}}
           [:a.button.is-static
            "行动"]]
          [:p.control
           {:style {:margin-right "1em"}}
           [:span.select
            [:select
             {:on-change #(reset! action (-> % .-target .-value))
              :value @action}
             [:option {:title "说话"} "说话"]
             [:option {:title "使用道具"} "使用"]]]]
          (case @action
            "说话" (list ^{:key "ci-voice-1"} [:p.control
                                             {:style {:margin-left "1em"}}
                                             [:a.button.is-static
                                              "声音"]]
                       ^{:key "ci-voice-2"} [:p.control
                                             [:span.select
                                              [:select
                                               {:on-change #(reset! voice (-> % .-target .-value))
                                                :value @voice}
                                               [:option {:title "同一舞台内的角色 能够 听到你说话"} "正常"]
                                               [:option {:title "相邻舞台的角色 可能会 听到你说话"} "大声"]
                                               [:option {:title "只有你选中的角色，以及通过聆听检定的角色能够听到你说话"} "小声"]]]]
                       (when (= @voice "小声")
                         (list
                          ^{:key "ci-voice-1"} [:p.control
                                                {:style {:margin-left "2em"}}
                                                [:a.button.is-static
                                                 "对象"]]
                          ^{:key "ci-voice-4"} [:> react-select
                                                {:placeholder "选择角色"
                                                 :value @voice-to-target
                                                 :isMulti true
                                                 :on-change #(let [res (map (fn [xs] (reduce (fn [a x]
                                                                                               (assoc a (keyword (first x)) (nth x 1)))
                                                                                             {}
                                                                                             (js->clj xs))) (vec %))]
                                                               (reset! voice-to-target (vec res)))
                                                 :options voice-options}])))
            "使用" (list ^{:key "ci-use-1"} [:p.control
                                           {:style {:margin-left "1em"}}
                                           [:a.button.is-static
                                            "物品"]]
                       ^{:key "ci-use-2"} [:> react-select
                                           {:placeholder "选择道具"
                                            :value @items-to-use
                                            :isMulti true
                                            :on-change #(let [res (map (fn [xs] (reduce (fn [a x]
                                                                                          (assoc a (keyword (first x)) (nth x 1)))
                                                                                        {}
                                                                                        (js->clj xs))) (vec %))]
                                                          (reset! items-to-use (vec res)))
                                            :options item-options}])
            nil)]
         [:div.field.has-addons
          [:div.control
           {:style {:width "80%"}}
           [:textarea.textarea {:style {:resize "none"}
                                :type "text"
                                :placeholder (case @action
                                               "说话" "说话"
                                               "使用" "描述你的使用方法"
                                               "")
                                :onKeyPress (fn [e]
                                              (when (and (= 13 (.-charCode e)) (not (str/blank? @text)))
                                                (send-msg avatar-id-use)
                                                (reset! text "")
                                                (.preventDefault e)))
                                :on-change (fn [e]
                                             (reset! text (-> e .-target .-value)))
                                :value @text}]]
          [:p.control
           [:a.button
            {:style {:height "100%" :margin-left "1px"}
             :on-click #(do
                          (send-msg avatar-id-use)
                          (reset! text ""))}
            (case @action
              "说话" "发言"
              "使用" "行动"
              "")]]]]))))

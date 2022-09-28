(ns cocdan.fragment.speak 
  (:require [cocdan.data.performer :as performer]))

"仅针对 NPC、 Avatar 和 KP 能调用 Speak"

(defn speak
  "生成 speak 组件的 HTML 代码"
  [{:keys [message mood]} avatar pos]
  (let [header (performer/header avatar mood)
        speaker-name (:name avatar)
        name-item [:div
                   {:class (case pos :left "chat-content-left" "chat-content-right")
                    :style {:padding 0}}
                   [:p {:style {:margin-bottom 0}} speaker-name]]
        text-item [:div
                   {:class (str "chat-content-outer " (case pos :left "chat-content-left" "chat-content-right"))}
                   [:div.chat-content
                    [:span (str message)]]]
        header-item [:div.chat-header-width
                     [:div
                      {:style {:margin-bottom 0
                               :width "100%"}}
                      [:div {:style (merge {:width "32px"
                                            :height "32px"
                                            :background (str "url(" header ")")
                                            :background-size "cover"}
                                           (case pos :left {:margin-left "auto"} {:margin-right "auto"}))}]]]]
    (case pos
      :right
      [:div
       {:style {:word-wrap "break-word"}}
       [:div {:style {:display "flex"}}
        [:div.chat-empty-width]
        name-item
        [:div.chat-header-width]]
       [:div {:style {:display "flex"}}
        [:div.chat-empty-width]
        text-item
        header-item]]
      [:div
       [:div {:style {:display "flex"}}
        [:div.chat-header-width]
        name-item]
       [:div {:style {:display "flex"}}
        header-item
        text-item]])))

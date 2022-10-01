(ns cocdan.fragment.speak 
  (:require [cocdan.data.performer.core :as performer]
            ["antd" :refer [Avatar]]))

"仅针对 NPC、 Avatar 和 KP 能调用 Speak"

(defn speak
  "生成 speak 组件的 HTML 代码"
  [{:keys [message mood]} avatar pos ack?] 
  (let [header (performer/header avatar mood)
        speaker-name (:name avatar)
        name-item [:div
                   {:class (case pos :left "chat-name-left" "chat-name-right")}
                   [:p {:style {:margin-bottom 0}} speaker-name]]
        text-item [:div
                   {:class (str "chat-content-outer " (case pos :left "chat-content-left" "chat-content-right"))}
                   [:div.chat-content
                    [:span (str message)]
                    [:span (if ack? " ACK" " WAIT")]]]
        header-item [:> Avatar {:src header}]]
    (case pos
      :right
      [:div.chat-content-line
       {:style {:word-wrap "break-word"}}
       name-item
       [:div {:style {:display "flex"}} 
        [:div {:style {:margin-left "auto"}}]
        text-item
        header-item]]
      [:div.chat-content-line
       name-item
       [:div {:style {:display "flex"}}
        header-item
        text-item]])))

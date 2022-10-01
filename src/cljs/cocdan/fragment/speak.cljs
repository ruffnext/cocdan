(ns cocdan.fragment.speak 
  (:require ["antd" :refer [Avatar Badge]]
            [cocdan.data.performer.core :as performer]
            [cocdan.data.transaction.speak :refer [Speak]]
            [cocdan.data.visualizable :refer [IVisualizable]]))

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
                    [:span (str message)]]]
        header-item (if ack?
                      [:> Badge {:dot true :status "success" :title (str "服务器已确认")}
                       [:> Avatar {:src header}]]
                      [:> Badge {:dot "载入中" :title "等待服务器返回确认"}
                       [:> Avatar {:src header}]])]
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

(extend-type Speak
  IVisualizable
  (to-hiccup [{:keys [avatar] :as this} ctx {:keys [viewpoint transaction]}]
    (let [avatar-record (get-in (:context/props ctx) [:avatars (keyword (str avatar))])]
      (speak this avatar-record (if (= viewpoint avatar) :right :left) (:ack transaction)))))

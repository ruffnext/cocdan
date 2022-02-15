(ns cocdan.components.chatting-input
  (:require
   [cocdan.core.chat :as chat]
   [reagent.core :as r]
   [clojure.string :as str]
   [re-posh.core :as rp]
   [re-frame.core :as rf]))

(defn chatting-input
  [stage-id avatar-avilables {avatar-id-use :id}]
  (when (not (nil? avatar-id-use))
    (r/with-let [text (r/atom "")
                 send-msg (fn [id]
                            (rf/dispatch [:event/chat-send-message stage-id (chat/make-msg id chat/msg @text)])
                            (reset! text ""))]
      [:div
       [:div {:class "field has-addons"}
        [:p.control
         [:span.select
          [:select
           {:on-change #(rp/dispatch [:rpevent/upsert :stage {:id stage-id
                                                              :current-use-avatar (js/parseInt (-> % .-target .-value))}])
            :value avatar-id-use}
           (for [avatar avatar-avilables]
             ^{:key (str "cias-" (:id avatar))} [:option {:value (:id avatar)} (str (:name avatar))])]]]
        [:p.control
         [:a.button {:on-click nil} "ACTION"]]
        [:p.control
         [:a.button {:on-click #(send-msg avatar-id-use)} "MESSAGE"]]]
       [:textarea.textarea {:type "text"
                            :on-key-press (fn [e]
                                            (when (and (= 13 (.-charCode e)) (not (str/blank? @text)))
                                              (send-msg avatar-id-use)))
                            :on-change #(reset! text (-> % .-target .-value))
                            :value @text}]])))

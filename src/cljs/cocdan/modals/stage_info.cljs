(ns cocdan.modals.stage-info
  (:require
   [reagent.core :as r]
   [markdown.core :refer [md->html]]
   [cocdan.auxiliary :as aux]))

(defonce active? (r/atom false))
(defonce stage (r/atom nil))

(defn- close-modal
  []
  (reset! active? false))

(aux/init-page
 {}
 {:event/modal-stage-info-active (fn [{:keys [db]} _ res]
                                   (if (nil? res)
                                     (reset! active? false)
                                     (swap! active? not))
                                   (reset! stage res)
                                   {:db db})})

(defn stage-info
  []
  (when (and (not (nil? @stage)) @active?)
    (let [{title :title
           banner :banner
           intro :introduction} @stage]
      [:div.modal {:class "is-active"}
       [:div.modal-background {:on-click close-modal}]
       [:div.modal-card
        [:header.modal-card-head
         [:p.modal-card-title title]
         [:button.delete {:on-click close-modal}]]
        [:div.modal-card-image
         [:figure {:class "image"}
          [:img {:src banner :style {:max-height "320px"
                                     :object-fit "cover"}}]]]
        [:section.modal-card-body>div.content
         [:div
          {:dangerouslySetInnerHTML {:__html (md->html intro)}}]]
        [:footer.modal-card-foot
         [:button {:class "button is-success"} "Get me in"]
         [:button.button {:on-click close-modal} "Cancel"]]]
       [:button {:class "modal-close is-large" :on-click close-modal}]])))
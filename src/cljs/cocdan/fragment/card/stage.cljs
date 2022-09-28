(ns cocdan.fragment.card.stage
  (:require [cocdan.database.main :refer [db]]
            [posh.reagent :as p]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]))

(defn card
  [stage-id] 
  (let [stage-id (str stage-id)
        {image :image
         _stage-id :id
         stage-name :name
         intro :introduction
         _substages :substages
         controller :controller} (:stage/props @(p/pull db '[:stage/props] stage-id))]
    [:div.card.stage-card
     [:div.card-image
      [:figure.image.is-4by3
       [:img {:src image}]]]
     [:div.card-content
      [:div.content
       [:p
        [:span.title {:style {:margin-right "0.5em"}} stage-name]
        [:span.tag.is-primary controller]]
       [:p intro]]]
     [:div.card-footer
      [:a.card-footer-item
       {:on-click (fn [_x] 
                    (rf/dispatch-sync [:event/stage-performing stage-id])
                    (rfe/push-state :main {:nav "performing"}))}
       "进入"]
      [:a.card-footer-item "设置"]
      [:a.card-footer-item "删除"]]]))

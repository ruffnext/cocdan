(ns cocdan.fragment.card.stage
  (:require [cocdan.core.api-posher :as api-posher]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]))

(defn card
  [stage-id] 
  (let [stage (api-posher/posh-stage-by-id stage-id)
        {image :image
         _stage-id :id
         stage-name :name
         intro :introduction
         _substages :substages
         controlled_by :controlled_by} stage]
    (if stage
      (let [avatar (api-posher/posh-avatar-by-id controlled_by)] 
        [:div.card.stage-card
         [:div.card-image
          [:figure.image.is-4by3
           [:img {:src (if (empty? image )
                         "/img/warning_clojure.png"
                         image)}]]]
         [:div.card-content
          [:div.content
           [:p
            [:span.title {:style {:margin-right "0.5em"}} stage-name]
            [:span.tag.is-primary (:name avatar)]]
           [:p intro]]]
         [:div.card-footer
          [:a.card-footer-item
           {:on-click (fn [_x]
                        (rf/dispatch-sync [:event/stage-performing stage-id])
                        (rfe/push-state :main {:nav "performing"}))}
           "进入"]
          [:a.card-footer-item "设置"]
          [:a.card-footer-item "删除"]]])
      [:p (str "无法获得舞台 id=" stage-id " 的信息" )])))
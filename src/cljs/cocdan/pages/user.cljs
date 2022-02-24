(ns cocdan.pages.user
  (:require
   [cocdan.components.avatar-simple :refer [component-avatars-list component-avatar-simple]]
   [cocdan.components.stage-simple :refer [component-stages-list]]
   [reitit.frontend.easy :as rfe]
   [cocdan.core.user :refer [posh-my-eid]]
   [cocdan.db :as gdb]
   [cocdan.core.avatar :refer [posh-my-avatars]]))

(defn component-user-info-detail
  [user]
  [:div
   [:h1.title "My Info"]
   [:div {:class "card-content"}
    [:div.media
     [:div.media-left
      [:figure {:class "image is-128x128"}
       [:img {:src "/img/header.png"}]]]
     [:div.media-content
      [:h2.title (:name user)]]]]])

(defn user-page
  [user]
  (let [avatar-eids @(posh-my-avatars gdb/db)]
    [:div.container
     [:section.section
      [:div.columns {:class "is-horizontal"}
       [:div.column {:class "is-two-third"}
        (component-user-info-detail user)]
       [:div.column {:class "is-one-third" :style {:margin-right "3em"}}
        [:h3.title {:class "is-4"} "Legendary Investigator"]
        (when (first avatar-eids)
          (component-avatar-simple (first avatar-eids)))]]]
     (component-stages-list avatar-eids)
     [:section.section
      [:h1.title "我的角色"]
      [:div.columns {:class "is-multiline" :style {:margin-left 12}}
       (component-avatars-list avatar-eids)]]]))

(defn page
  []
  (let [user @(posh-my-eid gdb/db)]
    (if user
      (user-page (gdb/d-pull-eid @gdb/db user))
      (rfe/push-state :login))))

(ns cocdan.components.avatar-simple
  (:require [re-frame.core :as rf]
            [cocdan.db :as gdb]
            [cocdan.core.stage :refer [posh-stage-by-id]]))

(defn component-avatar-simple
  [avatar-eid]
  (let [{id :id name :name on_stage :on_stage header :header :as avatar} (gdb/pull-eid gdb/db avatar-eid)
        {stage-title :title
         stage-id :id
         stage-admin :owned_by} (when (not (nil? on_stage)) (->> @(posh-stage-by-id gdb/db on_stage)
                                                                 (gdb/pull-eid gdb/db)))]
    [:div.card
     [:div.card-content
      [:div.media
       [:div.media-left
        [:figure {:class "image is-48x48"}
         [:a {:on-click #(rf/dispatch [:event/modal-coc-avatar-edit-active [avatar] id])}
          [:img {:src header :style {:object-fit "cover"}}]]]]
       [:div.media-content
        [:p {:class "title is-4"} [:a {:on-click #(rf/dispatch [:event/modal-coc-avatar-edit-active [avatar] id])} name] (when (= stage-admin id)
                                                                   [:span {:class "subtitle is-7"} " 管理员"])]
        [:p {:class "subtitle is-6"} (if (nil? on_stage)
                                       "无所属"
                                       [:a {:href (str "#/stage/" stage-id)}
                                        (str "@" stage-title)])]]]
      [:div.content
       [:div {:class "columns is-multiline has-text-centered"}
        [:div {:class "column is-one-quarter"}
         [:span "HP"]
         [:br]
         [:span "9/11"]]
        [:div {:class "column is-one-quarter"}
         [:span "MP"]
         [:br]
         [:span "9/11"]]
        [:div {:class "column is-one-quarter"}
         [:span "SAN"]
         [:br]
         [:span "9/11"]]]]]]))

(defn component-create-avatar-simple
  []
  [:div.card {:style {:height "100%"}}
   [:div.card-content {:class "columns is-vcentered"
                       :style {:margin 0
                               :height "100%"}}
    [:div {:class "column has-text-centered"
           :style {:width "100%"}}
     [:div
      [:span.icon {:class "is-large"} [:i {:class "fas fa-plus fa-3x"}]]
      [:p {:class "subtitle is-5"} "Create One"]]]]])


(defn component-avatars-list
  [avatars]
  (list (doall
         (map-indexed (fn [i v] (with-meta [:div {:class "column component-avatar-simple"} (component-avatar-simple v)] {:key i})) avatars))
        (with-meta [:div {:class "column component-avatar-simple"}
                    (component-create-avatar-simple)] {:key "new-avatar"})))

(ns cocdan.components.coc.avatar-edit
  (:require
   [reagent.core :as r]
   [cocdan.auxiliary :as gaux]
   ["react-select/creatable" :refer (default) :rename {default react-select}]
   [re-frame.core :as rf]))

(defonce active? (r/atom false))
(defonce avatars (r/atom {}))
(defonce current-edit-avatar-index (r/atom ""))

(gaux/init-page
 {}
 {:event/modal-coc-avatar-edit-active
  (fn [_db _driven-by avatars' current-edit-avatar-id']
    (reset! active? true)
    (reset! avatars (sort-by :name avatars'))
    (if (nil? current-edit-avatar-id')
      (reset! current-edit-avatar-index 0)
      (reset! current-edit-avatar-index (let [index-list (vec (for [a avatars']
                                                                (:id a)))]
                                          (if (contains? (set index-list) current-edit-avatar-id')
                                            (.indexOf index-list current-edit-avatar-id')
                                            0))))
    {})})

(defn- edit-cancel
  []
  (reset! active? false))

(defn coc-avatar-edit
  []
  (when @active?
    (let [deref-avatars @avatars
          avatar-options (map-indexed (fn [i {name :name}]
                                        {:value i :label name})
                                      deref-avatars)
          avatar (nth deref-avatars @current-edit-avatar-index)
          on-create (fn [id']
                      (js/console.log id')
                      (swap! avatars #(assoc-in %
                                              [:attributes :substages (keyword id')]
                                              (assoc ((keyword @current-edit-avatar-index) (-> @avatars :attributes :substages))
                                                     :name id')))
                      (reset! current-edit-avatar-index id'))]
      [:div.modal {:class "is-active"}
       [:div.modal-background {:on-click edit-cancel}]
       [:div.modal-card
        [:header.modal-card-head
         [:p "COC Avatar Editor"]
         [:div.field-body>div.field>div.control
          {:style {:padding-left "2em"
                   :padding-right "2em"}}
          [:> react-select
           {:placeholder "Select a avatar"
            :value (nth avatar-options @current-edit-avatar-index)
            :onCreateOption on-create
            :on-change #(reset! current-edit-avatar-index (.-value %))
            :options (into [] avatar-options)}]]]
        [:section.modal-card-body
         {:style {:min-height "40vh"}}
         [:p.subtitle.is-6 "基本内容"]
         [:div.field.is-horizontal
          [:div.field-label.is-normal
           [:label "名称"]]
          [:div.field-body>div.field
           [:input.input
            {:value (:name avatar)
             :on-change #(swap! avatars (fn [x] (assoc-in x [@current-edit-avatar-index :name] (-> % .-target .-value))))}]]]
         [:hr]
         [:p.subtitle.is-6 "背包物品"]]
        [:footer.modal-card-foot
         [:button.button {:class "is-primary"
                          :on-click #(do
                                       (let [stage' @avatars
                                             to-submit {:id (:id stage')
                                                        :attributes (:attributes stage')}]
                                         (rf/dispatch [:event/patch-to-server :stage to-submit]))
                                       (edit-cancel))} "Submit"]
         [:button.button {:on-click edit-cancel} "Cancel"]]]])))
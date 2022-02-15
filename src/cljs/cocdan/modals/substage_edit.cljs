(ns cocdan.modals.substage-edit
  (:require
   [reagent.core :as r]
   [cocdan.auxiliary :as gaux]
   ["react-select/creatable" :refer (default) :rename {default react-select}]
   [re-frame.core :as rf]))

(defonce active? (r/atom false))
(defonce stage (r/atom {}))
(defonce current-edit-substage-id (r/atom ""))

(gaux/init-page
 {}
 {:event/modal-substage-edit-active (fn [_db _driven-by stage' current-edit-substage']
                                      (reset! active? true)
                                      (reset! stage stage')
                                      (if (nil? current-edit-substage')
                                        (reset! current-edit-substage-id (-> stage' :attributes :substages))
                                        (reset! current-edit-substage-id (name current-edit-substage')))
                                      {})})

(defn- edit-cancel
  []
  (reset! active? false))

(defn substage-edit
  []
  (when @active?
    (let [substage-options (for [[id' {name :name}] (into (sorted-map-by >) (-> @stage :attributes :substages))]
                             {:value (subs (str id') 1) :label name})
          substage ((keyword @current-edit-substage-id) (-> @stage :attributes :substages))
          connected-substage (sort (disj (set (for [[k _v] (-> @stage :attributes :substages)]
                                                (subs (str k) 1)))
                                         @current-edit-substage-id))
          _ (js/console.log connected-substage)
          on-create (fn [id']
                      (js/console.log id')
                      (swap! stage #(assoc-in %
                                              [:attributes :substages (keyword id')]
                                              (assoc ((keyword @current-edit-substage-id) (-> @stage :attributes :substages))
                                                     :name id')))
                      (reset! current-edit-substage-id id'))]
      [:div.modal {:class "is-active"}
       [:div.modal-background {:on-click edit-cancel}]
       [:div.modal-card
        [:header.modal-card-head
         [:p "substage editor"]
         [:div.field-body>div.field>div.control
          {:style {:padding-left "2em"
                   :padding-right "2em"}}
          [:> react-select
           {:placeholder "Select a substage"
            :value (first (take 1 (filter #(= (:value %) @current-edit-substage-id) substage-options)))
            :onCreateOption on-create
            :on-change #(reset! current-edit-substage-id (.-value %))
            :options (into [] substage-options)}]]]
        [:section.modal-card-body
         {:style {:min-height "40vh"}}
         [:div.field.is-horizontal
          [:div.field-label.is-normal
           [:label "名称"]]
          [:div.field-body>div.field
           [:input.input
            {:value (:name substage)
             :on-change #(swap! stage (fn [x] (assoc-in x [:attributes :substages (keyword @current-edit-substage-id) :name] (-> % .-target .-value))))}]]]
         [:div.field.is-horizontal
          [:div.field-label.is-normal
           [:label "可达区域"]]
          [:div.field-body>div.field
           [:div.select {:class "is-multiple"}
            [:select {:multiple true
                      :size 8
                      :style {:max-height "10em"}
                      :value (let [val (-> substage :coc :连通区域)
                                   seqval (seq val)]
                               (if (nil? seqval)
                                 []
                                 seqval))
                      :on-change #()
                      :on-click (fn [e]
                                  (let [value (-> e .-target .-value)
                                        ori-value (-> substage :coc :连通区域)]
                                    (if (contains? ori-value value)
                                      (swap! stage (fn [a] (assoc-in a [:attributes :substages (keyword @current-edit-substage-id) :coc :连通区域] (disj ori-value value))))
                                      (swap! stage (fn [a] (assoc-in a [:attributes :substages (keyword @current-edit-substage-id) :coc :连通区域] (conj ori-value value)))))))}
             (doall (map
                     (fn [v] (with-meta [:option
                                         (str v)] {:key (str "se-s-" v)}))
                     connected-substage))]]]]]
        [:footer.modal-card-foot
         [:button.button {:class "is-primary"
                          :on-click #(do
                                       (let [stage' @stage
                                             to-submit {:id (:id stage')
                                                        :attributes (:attributes stage')}]
                                         (rf/dispatch [:event/patch-to-server :stage to-submit]))
                                       (edit-cancel))} "Submit"]
         [:button.button {:on-click edit-cancel} "Cancel"]]]])))
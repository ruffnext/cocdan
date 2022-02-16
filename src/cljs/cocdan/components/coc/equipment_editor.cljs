(ns cocdan.components.coc.equipment-editor
  (:require
   [reagent.core :as r]
   ["react-select/creatable" :refer (default) :rename {default react-select}]))

(defn- display
  [items {avatar-id :id}]
  [:div
   [:p "你有如下的装备"]
   (for [[loc items'] items]
     (let [options (for [{name :name} items']
                     {:value name :label name})]
       (with-meta [:div.field.is-horizontal
                   [:div.field-label.is-normal
                    [:label (name loc)]]
                   [:div.field-body>div.field
                    [:> react-select
                     {:placeholder "Select a avatar"
                      :value options
                      :isMulti true
                      :on-change #(js/console.log %)
                      :options options}]]]
         {:key (str avatar-id "ee" loc)})))])

(defn- edit
  []
  [:div.field.has-addons
   [:p.control
    [:span.select>select
     [:option "头部"]
     [:option "颈部"]]
    [:span.select>select
     [:option "显露"]
     [:option "遮蔽"]
     [:option "隐藏"]]]
   [:p.control
    [:input.input]]
   [:p.control
    [:a.button
     "增加"]]])

(defn jsx->clj
  [x]
  (into {} (for [k (.keys js/Object x)] [k (aget x k)])))

(defn coc-equipment-editor
  [{on-change :on-change} {avatar-id :id :as avatar}]
  (let [items (-> avatar :attributes :coc :items)]
    (r/with-let [current-location (r/atom "头部")
                 current-hidden (r/atom "显露")
                 current-input (r/atom "")]
      [:div
       {:style {:margin-left "1em"}}
       [:div
        (for [[loc items'] items]
          (let [options (for [{name :name
                               hidden? :hidden? :as item} items']
                          {:value item :label (str name " ( " hidden? " ) ")})]
            (when (seq options)
              (with-meta [:div.field.is-horizontal
                          [:div.field-label.is-normal
                           [:label (name loc)]]
                          [:div.field-body>div.field
                           [:> react-select
                            {:placeholder "Select a avatar"
                             :value options
                             :isMulti true
                             :on-change #(let [parseFunc (fn [xs] (reduce (fn [a [k v]]
                                                                            (assoc a (keyword k) v))
                                                                          {}
                                                                          (js->clj (.-value xs))))
                                               res (vec (map parseFunc %))]
                                           (on-change loc res))
                             :options options}]]]
                {:key (str avatar-id "ee" loc)}))))]
       [:div.field.has-addons
        {:style {:padding-left "2em"
                 :padding-right "2em"
                 :padding-top "3em"}}
        [:p.control
         [:span.select>select
          {:on-change #(reset! current-location (-> % .-target .-value))
           :value @current-location}
          [:option "头部"]
          [:option "面部"]
          [:option "颈部"]
          [:option "肩部"]
          [:option "胸前"]
          [:option "背后"]
          [:option "腰部"]
          [:option "衣兜"]
          [:option "内衬"]
          [:option "裤兜"]
          [:option "服装（其他）"]
          [:option "左臂"]
          [:option "右臂"]
          [:option "左腿"]
          [:option "右腿"]
          [:option "左手持"]
          [:option "右手持"]
          [:option "双手持"]
          [:option "躯干（其他）"]
          [:option "背包"]]
         [:span.select>select
          {:on-change #(reset! current-hidden (-> % .-target .-value))
           :value @current-hidden}
          [:option "显露"]
          [:option "遮蔽"]
          [:option "隐藏"]]]
        [:p.control.is-expanded
         [:input.input
          {:on-change #(reset! current-input (-> % .-target .-value))
           :value @current-input}]]
        [:p.control
         [:a.button
          {:on-click #(let [loc-items ((keyword @current-location) items)]
                        (on-change (keyword @current-location) (-> loc-items
                                                                   (conj {:name @current-input
                                                                          :hidden? @current-hidden})
                                                                   vec))
                        (reset! current-input ""))}
          "增加"]]]])))

(comment
  (let [tmp (list {"name" "123", "hidden?" "显露"})]
    (for [v tmp]
      (js->clj (-> v js/JSON.stringify js/JSON.parse)))))
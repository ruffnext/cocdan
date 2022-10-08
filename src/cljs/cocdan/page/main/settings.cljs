(ns cocdan.page.main.settings 
  (:require ["antd" :refer [Select]] 
            [cocdan.core.settings :refer [posh-setting-key-and-values
                                          update-setting-value-by-key]]
            [cocdan.aux :as data-aux]))

(defn page-show
  []
  [:p "演出设置页面"])


(defn setting-item-elem
  [k setting-name val]
  (let [val-elem
        (cond
          (nil? val) "nil"

          (boolean? val)
          [:label.checkbox
           [:input
            {:type "checkbox"
             :checked val
             :on-change #(update-setting-value-by-key k (not val))}]
           [:span {:style {:padding-left "0.5em"}} (str val)]]

          (or (list? val) (vector? val))
          [:div.control
           [:> Select
            {:isDisabled (or (object? (first val)) (record? (first val)))
             :value (map (fn [x] [:> (.-Option Select) {:label (str x) :value x}]) val)
             :onChange (fn [x]
                         (->> (js->clj x :keywordize-keys true)
                              (map :value)
                              (#(if (keyword? (first val))
                                  (map keyword %) %))
                              vec
                              (update-setting-value-by-key k)))
             :isMulti true}]]

          (set? val)
          [:div.control
           [:> Select
            {:isDisabled (or (object? (first val)) (record? (first val)))
             :value (map (fn [x] [:> (.-Option Select) {:label (str x) :value x}]) val)
             :onChange (fn [x]
                         (->> (js->clj x :keywordize-keys true)
                              (map :value)
                              (#(if (keyword? (first val))
                                  (map keyword %) %))
                              set
                              (update-setting-value-by-key k)))
             :isMulti true}]]

          (integer? val)
          [:div.control>input.input
           {:type "number"
            :value val
            :on-change (fn [event]
                         (let [res (-> event .-target .-value js/parseInt)]
                           (when (not (js/isNaN res))
                             (update-setting-value-by-key k res))))}]

          (float? val)
          [:div.control>input.input
           {:type "tel"
            :value val
            :on-change (fn [event]
                         (let [res (-> event .-target .-value js/parseFloat)]
                           (when (not (js/isNaN res))
                             (update-setting-value-by-key k res))))}]

          (instance? js/Date val)
          [:div.control
           [:input.input
            {:type "datetime-local"
             :value (data-aux/datetime-to-string val)
             :on-change #(update-setting-value-by-key
                          k
                          (data-aux/datetime-string-to-datetime
                           (-> % .-target .-value)))}]]

          (string? val) [:div.control
                         [:input.input
                          {:type "text"
                           :value val
                           :on-change #(update-setting-value-by-key k (-> % .-target .-value))}]]

          :else (str val))]
    [:tr
     [:th.is-vcentered {:title (str k)} setting-name]
     [:th.is-vcentered val-elem]]))

(defn settings-box-container
  [prefix items]
  [:div.card
   {:style {:margin-top "2em"
            :margin-bottom "2em"}}
   [:header.card-header
    [:p.card-header-title (str prefix " 设置")]]
   [:div.card-content
    [:table.table.is-fullwidth
     [:thead
      [:tr
       [:th "KEY"]
       [:th "VALUE"]]]
     [:tbody
      items]]]])

(defn settings-box
  [prefix settings-set]
  [settings-box-container
   prefix
   (doall
    (map
     (fn [[k n v]]
       (with-meta
         (setting-item-elem k n v)
         {:key k}))
     settings-set))])

(defn page
  []
  [:div.container
   (doall
    (map-indexed
     (fn [i [prefix kv-list]]
       (with-meta
         (settings-box prefix (sort-by #(name (first %)) kv-list))
         {:key i}))
     (into
      (sorted-map)
      (group-by #(-> % first namespace) @(posh-setting-key-and-values)))))])

(ns cocdan.pages.bulma
  (:require
   ["react-select/creatable" :refer (default) :rename {default react-select}]
   [reagent.core :as r]))

(defn page
  []
  [:section {:class "section"}
   [:div.container
    [:h1.title "Hello World"]
    [:p.subtitle "My first website with " [:strong "Hiccup"]]
    [:button.button "Button"]
    [:p
     [:span "This is a small "]
     [:span.icon [:i {:class "fas fa-envelope"}]]
     [:span " house"]]
    (r/with-let [values (r/atom [])
                 selected (r/atom [])]
      (let [mk-option (fn [v] {:value v :label v})
            options (map mk-option @values)]
        (js/console.log options)
        [:> react-select
         {:onCreateOption #(do
                             (swap! values (fn [v] (conj v %)))
                             (swap! selected (fn [v] (conj v (mk-option %)))))
          :onChange #(reset! selected %)
          :value @selected
          :isMulti true
          :options options}]))]])
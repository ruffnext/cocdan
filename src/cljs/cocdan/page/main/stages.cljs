(ns cocdan.page.main.stages
  (:require [cocdan.fragment.card.stage :as card-stage]))

(defn page-list
  []
  [:div.container
   [:div.columns>div.column.is-half.is-offset-one-quarter
    [:div.section
     [card-stage/card 1]]]])

(defn page-joined
  []
  [:p "我参加的舞台"])
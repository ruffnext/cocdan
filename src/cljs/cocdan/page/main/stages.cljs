(ns cocdan.page.main.stages
  (:require [cocdan.database.main :as main-db]
            [cocdan.fragment.card.stage :as card-stage]))

(defn page-list
  []
  (let [stage-ids @(main-db/posh-stage-ids)]
    [:div.container
     [:div.columns>div.column.is-half.is-offset-one-quarter
      [:div.section
       (doall
        (for [id stage-ids]
          (with-meta
            [card-stage/card id]
            {:key id})))]]]))

(defn page-joined
  []
  [:p "我参加的舞台"])
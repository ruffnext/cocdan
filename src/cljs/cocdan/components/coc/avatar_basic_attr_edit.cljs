(ns cocdan.components.coc.avatar-basic-attr-edit 
  (:require [cocdan.components.table-input :refer [input-int-with-addon]]))

(defn coc-avatar-basic-attr-edit
  [ref-avatar]
  [:table.table.is-bordered
   {:style {:max-width "100%"
            :margin-left "0.5em"
            :margin-right "0.5em"}}
   [:thead
    [:tr>td {:style {:width "100%"} :col-span 6} "属性"]]
   [:tbody
    [:tr
     [:th.nested-th {:style {:width "20%"}} "力量"]
     [:td.no-padding {:style {:width "30%"}} (input-int-with-addon ref-avatar [:attributes :coc :attrs :str] [:attributes :coc :attrs :str-correction])]
     [:th.nested-th {:style {:width "20%"}} "敏捷"]
     [:td.no-padding {:style {:width "30%"}} (input-int-with-addon ref-avatar [:attributes :coc :attrs :dex] [:attributes :coc :attrs :dex-correction])]]
    [:tr
     [:th.nested-th {:style {:width "20%"}} "意志"]
     [:td.no-padding {:style {:width "30%"}} (input-int-with-addon ref-avatar [:attributes :coc :attrs :pow] [:attributes :coc :attrs :dex-correction])]
     [:th.nested-th {:style {:width "20%"}} "体质"]
     [:td.no-padding {:style {:width "30%"}} (input-int-with-addon ref-avatar [:attributes :coc :attrs :con] [:attributes :coc :attrs :con-correction])]]
    [:tr
     [:th.nested-th {:style {:width "20%"}} "外貌"]
     [:td.no-padding {:style {:width "30%"}} (input-int-with-addon ref-avatar [:attributes :coc :attrs :app] [:attributes :coc :attrs :app-correction])]
     [:th.nested-th {:style {:width "20%"}} "教育"]
     [:td.no-padding {:style {:width "30%"}} (input-int-with-addon ref-avatar [:attributes :coc :attrs :edu] [:attributes :coc :attrs :edu-correction])]]
    [:tr
     [:th.nested-th {:style {:width "20%"}} "体型"]
     [:td.no-padding {:style {:width "30%"}} (input-int-with-addon ref-avatar [:attributes :coc :attrs :siz] [:attributes :coc :attrs :siz-correction])]
     [:th.nested-th {:style {:width "20%"}} "智力"]
     [:td.no-padding {:style {:width "30%"}} (input-int-with-addon ref-avatar [:attributes :coc :attrs :int] [:attributes :coc :attrs :int-correction])]]
    [:tr
     [:th.nested-th {:style {:width "20%"}} "移动"]
     [:td.no-padding {:style {:width "30%"}} (input-int-with-addon ref-avatar [:attributes :coc :attrs :mov] [:attributes :coc :attrs :mov-correction])]
     [:th.nested-th {:style {:width "20%"}} "幸运"]
     [:td.no-padding {:style {:width "30%"}} (input-int-with-addon ref-avatar [:attributes :coc :attrs :luck] [:attributes :coc :attrs :luck-correction])]]]])
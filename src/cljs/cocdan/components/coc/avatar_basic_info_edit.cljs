(ns cocdan.components.coc.avatar-basic-info-edit 
  (:require [cocdan.components.table-input :refer [input-str select-str input-date]]
            [cocdan.core.avatar :refer [get-avatar-attr]]
            [cocdan.core.coc :refer [posh-occupation-name-list]]))

(defn coc-avatar-basic-info-editor
  [ref-avatar ref-check-avatar]
  [:table.table.is-bordered
   {:style {:max-width "100%"
            :margin-left "0.5em"
            :margin-right "0.5em"}}
   [:thead
    [:tr>td {:style {:width "100%"} :col-span 4} "基础信息"]]
   [:tbody
    [:tr
     [:th.nested-th {:style {:width "20%"}} "姓名"]
     [:td.no-padding {:style {:width "80%"} :col-span 3} (input-str ref-avatar ref-check-avatar [:name])]]
    [:tr
     [:th.nested-th {:style {:width "20%"}} "性别"]
     [:td.no-padding {:style {:width "30%"}} (select-str ref-avatar ref-check-avatar [:attributes :gender] ["其他" "男" "女"] "gender")]
     [:th.nested-th {:style {:width "20%"}} "生日"]
     [:td.no-padding {:style {:width "30%"}} (input-date ref-avatar ref-check-avatar [:attributes :birthday])]]
    [:tr
     [:th.nested-th {:style {:width "20%"}} "年龄"]
     [:td.no-padding {:style {:width "30%"}} [:input.input.table-input {:disabled true :value (let [res (get-avatar-attr @ref-avatar [:age])]
                                                                                                (if (pos-int? res)
                                                                                                  res
                                                                                                  ""))}]]
     [:th.nested-th {:style {:width "20%"}} "现时间"]
     [:td.no-padding {:style {:width "30%"}} (input-date ref-avatar ref-check-avatar [:attributes :current-date])]]
    [:tr
     [:th.nested-th {:style {:width "20%"}} "故乡"]
     [:td.no-padding {:style {:width "30%"}} (input-str ref-avatar ref-check-avatar [:attributes :homeland])]
     [:th.nested-th {:style {:width "20%"}} "职业"]
     [:td.no-padding {:style {:width "30%"}} (select-str ref-avatar ref-check-avatar [:attributes :coc :attrs :occupation-name] (sort @(posh-occupation-name-list)) "选择职业")]]
    [:tr
     [:th.nested-th {:style {:width "20%"}} "住址"]
     [:td.no-padding {:style {:width "80%"} :col-span 3} [:input.input.table-input]]]]])
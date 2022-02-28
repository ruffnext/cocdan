(ns cocdan.pages.avatar 
  (:require
   [cocdan.db :refer [db]]
   [cocdan.core.avatar :refer [posh-avatar-by-id default-avatar]]
   [reagent.core :as r]
   [datascript.core :as d]
   [clojure.core.async :refer [go <!]]
   [cljs-http.client :as http]
   [cocdan.auxiliary :refer [remove-db-perfix]]
   [re-frame.core :as rf]))

(defn- input-int-with-addon
  [attr path]
  [:div.field.has-addons
   [:div.control
    [:input.input.table-input-centered
     {:style {:padding 0}
      :on-change (fn [event] (swap! attr #(assoc-in % path (js/parseInt (-> event .-target .-value)))))
      :type "tel"
      :default-value (reduce (fn [a f]
                               (f a)) @attr path)}]]
   [:div.control
    [:a.button.is-static.table-input-addons 
     {:style {:margin-left "2px"}}
     "±0"]]])

(defn- input-str
  ([attr path default-val]
   (let [current-val (reduce (fn [a f] (f a)) @attr path)]
     (when-not current-val (swap! attr #(assoc-in % path default-val)))
     [:input.input.table-input
      {:on-change (fn [event] (swap! attr #(assoc-in % path (-> event .-target .-value))))
       :type "text"
       :default-value current-val}]))
  ([attr path]
   (input-str attr path "")))

(defn- select-str
  [attr path candidates k]
  (let [current-val (or (reduce (fn [a f] (f a)) @attr path) (first candidates))]
    [:select.select.table-select 
     {:on-change (fn [event] (swap! attr #(assoc-in % path (-> event .-target .-value))))
      :value current-val
      :style {:height "3em"}}
     (doall (for [candidate candidates]
              ^{:key (str "ss-" k "-" candidate)}[:option (str candidate)]))]))

(defn page
  [{id :id}]
  (let [avatar (r/atom default-avatar)
        submit (fn []
                 (case id
                   "0" (rf/dispatch [:http-event/create-avatar @avatar])
                   (rf/dispatch [:event/patch-to-server :avatar @avatar])))]
    (r/create-class
     {:display-name "avatar-page"
      
      :component-did-update
      (fn [this [_ {old-avatar-id :id}]]
        (let [[{new-avatar-id :id}] (rest (r/argv this))]
          (when (not= old-avatar-id new-avatar-id)
            (let [avatar-id (js/parseInt new-avatar-id)
                  eid @(posh-avatar-by-id db avatar-id)
                  new-avatar (cond
                               (= 0 avatar-id) default-avatar
                               (not (nil? eid)) (remove-db-perfix (d/pull @db '[*] eid))
                               :else nil)]
              (if new-avatar
                (reset! avatar new-avatar)
                (go (let [res (<! (http/get (str "/api/avatar/a" avatar-id)))]
                      (when (= (:status res) 200)
                        (reset! avatar (:body res))))))))))
      
      :reagent-render
      (fn [{_id :id}]
        [:div.container>div.section>div.card
         [:div.container
          {:style {:padding-top "2em"
                   :padding-bottom "2em"
                   :padding-left "1em"
                   :padding-right "2em"}}
          [:div.columns.is-horizontal
           [:div.column.has-text-centered.nested-column.is-half
            [:table.table.is-bordered
             {:style {:max-width "100%"
                      :margin-left "0.5em"
                      :margin-right "0.5em"}}
             [:thead
              [:tr>td {:style {:width "100%"} :col-span 4} "基础信息"]]
             [:tbody
              [:tr
               [:th.nested-th {:style {:width "20%"}} "姓名"]
               [:td.no-padding {:style {:width "80%"} :col-span 3} (input-str avatar [:name])]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "故乡"]
               [:td.no-padding {:style {:width "30%"}} (input-str avatar [:attributes :homeland])]
               [:th.nested-th {:style {:width "20%"}} "性别"]
               [:td.no-padding {:style {:width "30%"}} (select-str avatar [:attributes :gender] ["男" "女" "其他"] "gender")]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "现时间"]
               [:td.no-padding {:style {:width "30%"}} [:input.input.table-input {:type "date"}]]
               [:th.nested-th {:style {:width "20%"}} "职业"]
               [:td.no-padding {:style {:width "30%"}} [:input.input.table-input]]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "生日"]
               [:td.no-padding {:style {:width "80%"} :col-span 3} [:input.input.table-input {:on-change (fn [event] (reset! avatar #(assoc-in % [:attributes :birthday] (-> event .-target .-value))))
                                                                                              :type "date"}]]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "住址"]
               [:td.no-padding {:style {:width "80%"} :col-span 3} [:input.input.table-input]]]]]]
           [:div.column.has-text-centered.nested-column.is-one-third
            [:table.table.is-bordered
             {:style {:max-width "100%"
                      :margin-left "0.5em"
                      :margin-right "0.5em"}}
             [:thead
              [:tr>td {:style {:width "100%"} :col-span 6} "属性"]]
             [:tbody
              [:tr
               [:th.nested-th {:style {:width "20%"}} "力量"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :str])]
               [:th.nested-th {:style {:width "20%"}} "敏捷"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :dex])]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "意志"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :pow])]
               [:th.nested-th {:style {:width "20%"}} "体质"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :con])]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "外貌"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :app])]
               [:th.nested-th {:style {:width "20%"}} "教育"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :edu])]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "体型"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :siz])]
               [:th.nested-th {:style {:width "20%"}} "智力"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :int])]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "移动"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :mov])]
               [:th.nested-th {:style {:width "20%"}} "幸运"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :luck])]]]]]
           [:div.column.has-text-centered.sketch {:style {:width "20%"}}
            [:img {:src (:header @avatar)}]]]
          [:div
           [:button.submit {:on-click submit} "提交"]]]])})))

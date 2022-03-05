(ns cocdan.pages.avatar 
  (:require
   [cocdan.db :refer [db]]
   [cocdan.core.avatar :refer [default-avatar get-avatar-attr]]
   [cocdan.core.coc :refer [complete-coc-avatar-attributes get-coc-attr set-coc-attr query-skill-by-name translate-skill-name-to-ch]]
   [reagent.core :as r]
   [datascript.core :as d]
   [clojure.core.async :refer [go <!]]
   [cljs-http.client :as http]
   [cocdan.auxiliary :refer [remove-db-perfix]]
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defonce skills (r/atom nil))
(defonce occupations (r/atom nil))

(comment
  @occupations
  )

(defn- input-int-with-addon
  [attr path path-addon]
  (let [value (reduce (fn [a f]
                        (f a)) @attr path)
        addon-value (reduce (fn [a f]
                              (f a)) @attr path-addon)]
    [:div.field.has-addons
     [:div.control
      [:input.input.table-input-centered
       {:style {:padding 0}
        :on-change (fn [event]
                     (let [new-avatar (assoc-in @attr path (let [res (-> event .-target .-value js/parseInt)]
                                                             (if (js/isNaN res)
                                                               0
                                                               res)))]
                       (reset! attr (complete-coc-avatar-attributes @attr new-avatar))))
        :type "tel"
        :value value}]]
     [:div.control
      [:a.button.is-static.table-input-addons
       {:style {:margin-left "2px"}}
       (cond
         (= 0 addon-value)  "±0"
         (pos-int? addon-value) (str "+" addon-value)
         (neg-int? addon-value) (str addon-value)
         :else "±0")]]]))

(defn- input-str
  ([attr _check-msg path default-val]
   (let [current-val (reduce (fn [a f] (f a)) @attr path)]
     (when-not current-val (swap! attr #(assoc-in % path default-val)))
     [:input.input.table-input
      {:on-change (fn [event]
                    (let [new-avatar (assoc-in @attr path (-> event .-target .-value))]
                      (reset! attr (complete-coc-avatar-attributes @attr new-avatar))))
       :type "text"
       :value current-val}]))
  ([attr check-msg path]
   (input-str attr check-msg path "")))

(defn- input-date
  ([attr _check-msg path default-val]
   (let [current-val (reduce (fn [a f] (f a)) @attr path)]
     (when-not current-val (swap! attr #(assoc-in % path (. (new js/Date default-val) getTime))))
     [:input.input.table-input
      {:type "date"
       :on-change (fn [event]
                    (let [new-avatar (assoc-in @attr path
                                               (. (new js/Date (-> event .-target .-value)) getTime))]
                      (reset! attr (complete-coc-avatar-attributes @attr new-avatar))))
       :value (if current-val
                (-> (. (new js/Date current-val) toISOString)
                    (str/split "T")
                    first)
                "")}]))
  ([attr check-msg path]
   (input-date attr check-msg path "1970-01-01")))

(defn- select-str
  [attr _check-avatar path candidates k]
  (let [current-val (reduce (fn [a f] (f a)) @attr path)]
    (if (and (nil? current-val) (seq candidates))
      (do
        (reset! attr (complete-coc-avatar-attributes @attr (assoc-in @attr path (first candidates))))
        nil)
      [:select.select.table-select
       {:on-change (fn [event]
                     (let [new-avatar (assoc-in @attr path (-> event .-target .-value))]
                       (reset! attr (complete-coc-avatar-attributes @attr new-avatar))))
        :value (or current-val "")
        :style {:height "3em"}}
       (doall (for [candidate candidates]
                ^{:key (str "ss-" k "-" candidate)} [:option (str candidate)]))])))

(defn- sum-skill-values
  [avatar skill-name]
  (let [base (or (get-coc-attr avatar (keyword (str skill-name "-base"))) 0)
        occupation (or (get-coc-attr avatar (keyword (str skill-name "-occupation"))) 0)
        interest (or (get-coc-attr avatar (keyword (str skill-name "-interest"))) 0)]
    (set-coc-attr avatar (keyword skill-name) (+ base occupation interest))))

(defn- skill-int-input
  [attr {initial-value :initial
         skill-name :skill-name} input-type]
  (let [item-keyword (keyword (str/join "-" [skill-name input-type]))
        current-val (get-coc-attr @attr item-keyword)
        on-change (fn [event]
                    (let [new-avatar (-> @attr
                                         (set-coc-attr item-keyword (let [res (-> event .-target .-value js/parseInt)]
                                                                      (if (js/isNaN res)
                                                                        0
                                                                        res)))
                                         (sum-skill-values skill-name))]
                      (reset! attr (complete-coc-avatar-attributes @attr new-avatar))))]
    (if (nil? current-val)
      (do
        (cond
          (and (= input-type "base") initial-value) (swap! attr #(-> % 
                                                                     (set-coc-attr item-keyword initial-value)
                                                                     (sum-skill-values skill-name)))
          :else (swap! attr #(set-coc-attr % item-keyword 0)))
        nil)
      [:input.input.table-input
       (merge {:on-change on-change
               :type "number"
               :value current-val
               :min 0
               :max 100}
              (case input-type
                "base" {:disabled true}
                {}))])))

(defn- gen-occupation-skill-table
  [attr skill-name]
  (let [skill (query-skill-by-name skill-name)
        skill-all (or (get-coc-attr @attr (keyword (:skill-name skill))) 0)]
    [:tr
     [:th (translate-skill-name-to-ch skill-name)]
     [:td.no-padding (skill-int-input attr skill "base")]
     [:td.no-padding (skill-int-input attr skill "occupation")]
     [:td.no-padding (skill-int-input attr skill "interest")]
     [:td.no-padding (str skill-all)]]))

(defn page
  [{id :id}]
  (r/with-let [avatar (r/atom default-avatar)
               check-avatar (r/atom {})
               submit (fn [avatar]
                        (case (:id avatar)
                          "0" (rf/dispatch [:http-event/create-avatar avatar])
                          (rf/dispatch [:event/patch-to-server :avatar avatar])))]
    (r/create-class
     {:display-name "avatar-page"

      :component-did-update
      (fn [this [_ {old-avatar-id :id}]]
        (let [[{new-avatar-id :id}] (rest (r/argv this))]
          (when (not= old-avatar-id new-avatar-id)
            (let [avatar-id (js/parseInt new-avatar-id)
                  eid (d/entid @db [:avatar/id avatar-id])
                  new-avatar (cond
                               (= 0 avatar-id) (complete-coc-avatar-attributes {} default-avatar)
                               (not (nil? eid)) (complete-coc-avatar-attributes @avatar (remove-db-perfix (d/pull @db '[*] eid)))
                               :else nil)]
              (if new-avatar
                (reset! avatar new-avatar)
                (go (let [res (<! (http/get (str "/api/avatar/a" avatar-id)))]
                      (when (= (:status res) 200)
                        (reset! avatar (:body res))))))))))

      :component-did-mount
      (fn [_this]
        (let [avatar-id (js/parseInt id)
              eid (d/entid @db [:avatar/id avatar-id])]
          (if (and (nil? eid) avatar-id)
            (go (let [res (<! (http/get (str "/api/avatar/a" avatar-id)))]
                  (when (= (:status res) 200)
                    (reset! avatar (complete-coc-avatar-attributes {} (:body res))))))
            (reset! avatar (-> (d/pull @db '[*] eid) remove-db-perfix (#(complete-coc-avatar-attributes  {} %)))))
          (when (nil? @skills)
            (go (let [res (<! (http/get "/api/files/res/docs%2Fcoc%2Fskills.csv"))]
                  (when (= (:status res) 200)
                    (reset! skills (:body res))))))
          (when (not (d/q '[:find ?occupation-name .
                            :where [?eid :coc-occupation/occupation-name ?occupation-name]]
                          @db))
            (rf/dispatch [:coc-event/refresh-occupations]))
          (when (not (d/q '[:find ?skill-name .
                            :where [?eid :coc-skill/skill-name ?skill-name]]
                          @db))
            (rf/dispatch [:coc-event/refresh-skills]))))

      :reagent-render
      (fn [{_id :id}]
        (js/console.log @avatar)
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
               [:td.no-padding {:style {:width "80%"} :col-span 3} (input-str avatar check-avatar [:name])]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "性别"]
               [:td.no-padding {:style {:width "30%"}} (select-str avatar check-avatar [:attributes :gender] ["其他" "男" "女"] "gender")]
               [:th.nested-th {:style {:width "20%"}} "生日"]
               [:td.no-padding {:style {:width "30%"}} (input-date avatar check-avatar [:attributes :birthday])]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "年龄"]
               [:td.no-padding {:style {:width "30%"}} [:input.input.table-input {:disabled true :value (let [res (get-avatar-attr @avatar [:age])]
                                                                                                          (if (pos-int? res)
                                                                                                            res
                                                                                                            ""))}]]
               [:th.nested-th {:style {:width "20%"}} "现时间"]
               [:td.no-padding {:style {:width "30%"}} (input-date avatar check-avatar [:attributes :current-date])]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "故乡"]
               [:td.no-padding {:style {:width "30%"}} (input-str avatar check-avatar [:attributes :homeland])]
               [:th.nested-th {:style {:width "20%"}} "职业"]
               [:td.no-padding {:style {:width "30%"}} (select-str avatar check-avatar [:attributes :coc :attrs :occupation-name] (sort (d/q '[:find [?occupation-name ...]
                                                                                                                                               :where [?eid :coc-occupation/occupation-name ?occupation-name]]
                                                                                                                                             @db)) "选择职业")]]
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
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :str] [:attributes :coc :attrs :str-correction])]
               [:th.nested-th {:style {:width "20%"}} "敏捷"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :dex] [:attributes :coc :attrs :dex-correction])]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "意志"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :pow] [:attributes :coc :attrs :dex-correction])]
               [:th.nested-th {:style {:width "20%"}} "体质"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :con] [:attributes :coc :attrs :con-correction])]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "外貌"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :app] [:attributes :coc :attrs :app-correction])]
               [:th.nested-th {:style {:width "20%"}} "教育"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :edu] [:attributes :coc :attrs :edu-correction])]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "体型"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :siz] [:attributes :coc :attrs :siz-correction])]
               [:th.nested-th {:style {:width "20%"}} "智力"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :int] [:attributes :coc :attrs :int-correction])]]
              [:tr
               [:th.nested-th {:style {:width "20%"}} "移动"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :mov] [:attributes :coc :attrs :mov-correction])]
               [:th.nested-th {:style {:width "20%"}} "幸运"]
               [:td.no-padding {:style {:width "30%"}} (input-int-with-addon avatar [:attributes :coc :attrs :luck] [:attributes :coc :attrs :luck-correction])]]]]]
           [:div.column.has-text-centered.sketch {:style {:width "20%"}}
            [:img {:src (:header @avatar)}]]]
          [:div.columns.is-horizontal
           [:div.column.is-one-third.has-text-centered>div.sketch
            {:style {:height "4em"}}
            (let [healthy (str (get-coc-attr @avatar :healthy))]
              [:p.icon.fas.fa-briefcase-medical.fa-2x
               {:class (if (not= "健康" healthy) "has-text-danger" "")
                :title (str (get-coc-attr @avatar :healthy))
                :style {:height "100%"
                        :width "100%"
                        :line-height "1.8em"}}
               [:span {:style {:padding-left "1em"}} (str "HP " (get-coc-attr @avatar :hp) " / " (get-coc-attr @avatar :max-hp))]])]
           [:div.column.is-one-third.has-text-centered>div.sketch
            {:style {:height "4em"}}
            (let [sanity (str (get-coc-attr @avatar :sanity))]
              [:p.icon.fa.fa-brain.fa-2x
               {:class (if (not= "清醒" sanity) "has-text-danger" "")
                :title (str (get-coc-attr @avatar :sanity))
                :style {:height "100%"
                        :width "100%"
                        :line-height "1.8em"}}
               [:span {:style {:padding-left "0.75em"}} (str "SAN " (get-coc-attr @avatar :san) " / " (get-coc-attr @avatar :max-san))]])]
           [:div.column.is-one-third.has-text-centered>div.sketch
            {:style {:height "4em"}}
            (let [mp (get-coc-attr @avatar :mp)
                  max-mp (get-coc-attr @avatar :max-mp)]
              [:p.icon.fa.fa-hat-wizard.fa-2x
               {:class (if (and (int? mp) (int? max-mp) (< mp (/ max-mp 2))) "has-text-danger" "")
                :style {:height "100%"
                        :width "100%"
                        :line-height "1.8em"}}
               [:span {:style {:padding-left "0.75em"}} (str "MP " (get-coc-attr @avatar :mp) " / " (get-coc-attr @avatar :max-mp))]])]]
          [:div.columns.is-horizontal
           [:div.column.is-half
            [:p.is-title {:style {:width "100%" :text-align "center"}} "本职技能"]
            [:table.table.is-bordered.has-text-centered
             {:style {:width "110%"}}
             [:thead
              [:tr
               [:td {:style {:width "27%"}} "技能名称"]
               [:td {:style {:width "17%"}} "基础"]
               [:td {:style {:width "17%"}} "本职"]
               [:td {:style {:width "17%"}} "兴趣"]
               [:td {:style {:width "22%"}} "成功率"]]]
             [:tbody
              (doall (for [vs (get-coc-attr @avatar :occupation-skills)]
                       (cond
                         (set? vs) nil
                         (= (first vs) "any") nil
                         :else (let [[skill-name _sub-skill] vs]
                                 (with-meta (gen-occupation-skill-table avatar skill-name) {:key (str "o-skill-" skill-name)})))))]]]
           [:div.column.is-half
            {:style {:padding-left "5%"}}
            [:p.is-title {:style {:width "100%" :text-align "center"}} "个人特长"]
            [:table.table.is-bordered.has-text-centered
             {:style {:width "100%"}}
             [:thead
              [:tr
               [:td "技能名称"]
               [:td "初始"]
               [:td "成长"]
               [:td "兴趣"]
               [:td "成功率"]]]]]
           [:div.column.is-half]]
          [:div
           [:button.submit {:on-click #(submit @avatar)} "提交"]]]])})))


(ns cocdan.components.coc.avatar-skill-edit
  (:require
   [reagent.core :as r]
   ["react-select" :refer (default) :rename {default react-select}]
   [cocdan.core.coc :refer [get-coc-attr
                            calc-coc-used-occupation-skill-points
                            list-occupation-skills-from-avatar
                            translate-skill-name-to-ch
                            posh-skill-name-list
                            query-skill-by-name
                            calc-coc-skill-success-rate
                            set-avatar-skill-success-rate
                            remove-coc-skill
                            complete-coc-avatar-attributes
                            list-avatar-all-coc-skills
                            calc-coc-used-interest-skill-points
                            avatar-add-coc-interest-skill
                            set-coc-attr]]
   [clojure.string :as str]))

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
                                         (set-coc-attr item-keyword (let [res (-> event .-target .-value js/parseInt)] (if (js/isNaN res) 0 res)))
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
  (let [{skill-name-eng :skill-name
         description :description :as skill} (query-skill-by-name skill-name)
        keyword-skill-name (keyword skill-name-eng)
        skill-all (get-coc-attr @attr keyword-skill-name)]
    (when (not= skill-all (calc-coc-skill-success-rate @attr skill-name-eng "occupation"))
      (swap! attr #(set-avatar-skill-success-rate % keyword-skill-name "occupation")))
    [:tr
     [:th {:title description} (translate-skill-name-to-ch skill-name)]
     [:td.no-padding (skill-int-input attr skill "base")]
     [:td.no-padding (skill-int-input attr skill "occupation")]
     [:td.no-padding (skill-int-input attr skill "interest")]
     [:td.no-padding (str skill-all)]]))

(defn- gen-interest-skill-table
  [attr skill-name]
  (let [{skill-name-eng :skill-name
         description :description :as skill} (query-skill-by-name skill-name)
        keyword-skill-name (keyword skill-name-eng)
        skill-all (or (get-coc-attr @attr keyword-skill-name) 0)
        on-remove-clicked (cond
                            (contains? #{"language(own)" "dodge"} skill-name-eng) nil
                            :else (fn []
                                    (let [new-avatar (remove-coc-skill @attr skill-name-eng)]
                                      (reset! attr (complete-coc-avatar-attributes @attr new-avatar)))))]
    (when (not= skill-all (calc-coc-skill-success-rate @attr skill-name-eng "interest"))
      (swap! attr #(set-avatar-skill-success-rate % skill-name-eng "interest")))
    [:tr
     [:th {:title description} [:span {:style {:margin-right "0.5em"}} (translate-skill-name-to-ch skill-name)] (when on-remove-clicked
                                                                                                                  [:sup {:style {:margin-right "-1em"}
                                                                                                                         :on-click on-remove-clicked}
                                                                                                                   [:span.icon.is-small [:i.fa.fa-trash]]])]
     [:td.no-padding (skill-int-input attr skill "base")]
     [:td.no-padding (skill-int-input attr skill "interest")]
     [:td.no-padding (str skill-all)]]))

(defn- gen-new-occupation-skill-options
  [avatar]
  (let [avatar-skills (set (list-avatar-all-coc-skills avatar))]
    (->>
     (get-coc-attr avatar :occupation-skills)
     (map-indexed (fn [i [v can-select & current-selected]]
                    (cond
                      (and (set? v) (not (contains? v "any")) (not= (count current-selected) can-select))
                      {:label (str "组" (inc i) " - 任选其" can-select) :options (reduce (fn [a skill-name]
                                                                                       (if (contains? avatar-skills skill-name)
                                                                                         a
                                                                                         (conj a {:label (translate-skill-name-to-ch skill-name)
                                                                                                  :value skill-name
                                                                                                  :group i}))) [] v)}

                      (and (set? v) (contains? v "any") (not= (count current-selected) can-select))
                      {:label (str "组" (inc i) " - 任选" can-select "项时代或个人特长") :options (reduce (fn [a skill-name]
                                                                                                 (if (contains? avatar-skills skill-name)
                                                                                                   a
                                                                                                   (conj a {:label (translate-skill-name-to-ch skill-name)
                                                                                                            :value skill-name
                                                                                                            :group i}))) [] @(posh-skill-name-list))}

                      :else nil)))
     (filter #(not (nil? %))))))

(defn coc-skill-editor
  [ref-avatar]
  [:div.columns.is-horizontal
   [:div.column.is-three-fifths
    {:style {:padding-right "2em"}}
    (let [remain-points (- (or (get-coc-attr @ref-avatar :occupation-skill-points) 0) (calc-coc-used-occupation-skill-points @ref-avatar))]
      (when (not= 0 remain-points)
        [:p.is-title {:style {:width "100%" :text-align "center"}} "本职技能"
         [:span "〔剩余本职技能点数 "]
         [:span.has-text-danger (str remain-points)]
         [:span "〕"]]))
    [:table.table.is-bordered.has-text-centered
     [:thead
      [:tr
       [:td {:style {:width "27%"}} "技能名称"]
       [:td {:style {:width "17%"}} "基础"]
       [:td {:style {:width "17%"}} "本职"]
       [:td {:style {:width "17%"}} "兴趣"]
       [:td {:style {:width "22%"}} "成功率"]]]
     [:tbody
      (doall (for [skill-name (list-occupation-skills-from-avatar @ref-avatar)]
               (with-meta (gen-occupation-skill-table ref-avatar skill-name) {:key (str "o-skill-" skill-name)})))
      [:tr>td.no-padding {:col-span 5}
       (r/with-let [addition-occupation-skills (r/atom [])]
         (let [options (gen-new-occupation-skill-options @ref-avatar)]
           [:> react-select
            {:placeholder (if (seq options) "您有可用的额外本职技能" "您没有可用的额外本职技能")
             :isMulti true
             :onChange (fn [res]
                         (let [current-selected (map (fn [x] {:value (.-value x)
                                                              :group (.-group x)}) res)
                               previous-selected-set (set @addition-occupation-skills)
                               current-selected-set (set current-selected)
                               added (filter #(not (contains? previous-selected-set %)) current-selected)
                               removed (filter #(not (contains? current-selected-set %)) previous-selected-set)
                               groups (get-coc-attr @ref-avatar :occupation-skills)
                               handled-add (reduce (fn [a {group :group
                                                           value :value}]
                                                     (assoc-in a [:attributes :coc :attrs :occupation-skills group] (concat (nth groups group) [value])))
                                                   @ref-avatar added)
                               handled-remove (reduce (fn [a {group :group
                                                              value :value}]
                                                        (let [current-group-value (filter #(not= value %) (nth (get-coc-attr a :occupation-skills) group))]
                                                          (assoc-in (remove-coc-skill a value) [:attributes :coc :attrs :occupation-skills group] current-group-value)))
                                                      handled-add removed)]
                           (reset! addition-occupation-skills current-selected)
                           (reset! ref-avatar (complete-coc-avatar-attributes @ref-avatar handled-remove))))
             :options options}]))]]]]
   [:div.column.is-two-fifths
    {:style {:padding-left "0"}}
    [:p.is-title {:style {:width "100%" :text-align "center"}} "个人特长"
     (let [remain-points (- (or (get-coc-attr @ref-avatar :interest-skill-points) 0) (calc-coc-used-interest-skill-points @ref-avatar))]
       (when (not= 0 remain-points)
         [:span
          [:span "〔剩余兴趣技能点数 "]
          [:span.has-text-danger (str remain-points)]
          [:span "〕"]]))]
    [:table.table.is-bordered.has-text-centered
     {:style {:width "100%"}}
     [:thead
      [:tr
       [:td {:style {:width "31%"}} "技能名称"]
       [:td {:style {:width "22%"}} "基础"]
       [:td {:style {:width "22%"}} "兴趣"]
       [:td {:style {:width "25%"}} "成功率"]]]
     [:tbody
      (doall (for [vs (get-coc-attr @ref-avatar :interest-skills)]
               (cond
                 (set? vs) nil
                 (= (first vs) "any") nil
                 :else (let [[skill-name _sub-skill] vs]
                         (with-meta (gen-interest-skill-table ref-avatar skill-name) {:key (str "i-skill-" skill-name)})))))
      [:tr
       [:td.no-padding {:col-span 4}
        (let [avatar-all-skills (set (list-avatar-all-coc-skills @ref-avatar))
              all-skills @(posh-skill-name-list)
              options (map (fn [skill-name]
                             {:value skill-name
                              :label (translate-skill-name-to-ch skill-name)})
                           (sort (filter #(not (contains? avatar-all-skills %)) all-skills)))]
          [:> react-select
           {:placeholder "新增个人特长"
            :style {:width "100%"}
            :on-change (fn [event]
                         (let [skill-name-eng (.-value event)]
                           (swap! ref-avatar #(complete-coc-avatar-attributes @ref-avatar (avatar-add-coc-interest-skill % skill-name-eng)))))
            :value ""
            :options options}])]]]]]
   [:div.column.is-half]])
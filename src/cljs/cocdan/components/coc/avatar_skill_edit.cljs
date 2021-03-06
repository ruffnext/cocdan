(ns cocdan.components.coc.avatar-skill-edit
  (:require
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
                                    (let [new-avatar (remove-coc-skill @attr skill-name-eng)
                                          res (complete-coc-avatar-attributes @attr new-avatar)]
                                      (js/console.log res)
                                      (reset! attr res))))]
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
                      (and (vector? v) (not (contains? (set v) "any")) (not= (count current-selected) can-select))
                      {:label (str "???" (inc i) " - ?????????" can-select) :options (reduce (fn [a skill-name]
                                                                                       (if (contains? avatar-skills skill-name)
                                                                                         a
                                                                                         (conj a {:label (translate-skill-name-to-ch skill-name)
                                                                                                  :value skill-name
                                                                                                  :group i}))) [] v)}

                      (and (vector? v) (contains? (set v) "any") (not= (count current-selected) can-select))
                      {:label (str "???" (inc i) " - ??????" can-select "????????????????????????") :options (reduce (fn [a skill-name]
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
        [:p.is-title {:style {:width "100%" :text-align "center"}} "????????????"
         [:span "??????????????????????????? "]
         [:span.has-text-danger (str remain-points)]
         [:span "???"]]))
    [:table.table.is-bordered.has-text-centered
     [:thead
      [:tr
       [:td {:style {:width "27%"}} "????????????"]
       [:td {:style {:width "17%"}} "??????"]
       [:td {:style {:width "17%"}} "??????"]
       [:td {:style {:width "17%"}} "??????"]
       [:td {:style {:width "22%"}} "?????????"]]]
     [:tbody
      (doall (for [skill-name (list-occupation-skills-from-avatar @ref-avatar)]
               (with-meta (gen-occupation-skill-table ref-avatar skill-name) {:key (str "o-skill-" skill-name)})))
      [:tr>td.no-padding {:col-span 5}
       (let [options (gen-new-occupation-skill-options @ref-avatar)
             current-selected (let [groups (get-coc-attr @ref-avatar :occupation-skills)
                                    selected-skills (map-indexed (fn [i [some-vec can-select & selected]]
                                                                   (if (and (vector? some-vec) (pos-int? can-select))
                                                                     (when (seq selected)
                                                                       (map (fn [x] {:value x
                                                                                     :label (translate-skill-name-to-ch x)
                                                                                     :group i}) selected))
                                                                     nil)) groups)]
                                (reduce into [] (filter #(not (nil? %)) selected-skills)))]
         [:> react-select
          {:placeholder (if (seq options) "?????????????????????????????????" "????????????????????????????????????")
           :isMulti true
           :onChange (fn [res]
                       (let [changed (map (fn [x] {:value (.-value x)
                                                            :label (.-label x)
                                                            :group (.-group x)}) res)
                             previous-selected-set (set current-selected)
                             current-selected-set (set changed)
                             added (filter #(not (contains? previous-selected-set %)) changed)
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
                         (js/console.log "ADD" added)
                         (js/console.log "REMOVE" removed)
                         (js/console.log handled-remove)
                         (reset! ref-avatar (complete-coc-avatar-attributes @ref-avatar handled-remove))))
           :options options
           :value current-selected}])]]]]
   [:div.column.is-two-fifths
    {:style {:padding-left "0"}}
    [:p.is-title {:style {:width "100%" :text-align "center"}} "????????????"
     (let [remain-points (- (or (get-coc-attr @ref-avatar :interest-skill-points) 0) (calc-coc-used-interest-skill-points @ref-avatar))]
       (when (not= 0 remain-points)
         [:span
          [:span "??????????????????????????? "]
          [:span.has-text-danger (str remain-points)]
          [:span "???"]]))]
    [:table.table.is-bordered.has-text-centered
     {:style {:width "100%"}}
     [:thead
      [:tr
       [:td {:style {:width "31%"}} "????????????"]
       [:td {:style {:width "22%"}} "??????"]
       [:td {:style {:width "22%"}} "??????"]
       [:td {:style {:width "25%"}} "?????????"]]]
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
           {:placeholder "??????????????????"
            :style {:width "100%"}
            :on-change (fn [event]
                         (let [skill-name-eng (.-value event)]
                           (swap! ref-avatar #(complete-coc-avatar-attributes @ref-avatar (avatar-add-coc-interest-skill % skill-name-eng)))))
            :value ""
            :options options}])]]]]]
   [:div.column.is-half]])
(ns cocdan.components.table-input 
  (:require [cocdan.core.coc :refer [complete-coc-avatar-attributes]]
            [clojure.string :as str]))

(defn select-str
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

(defn input-str
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

(defn input-date
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

(defn input-int-with-addon
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

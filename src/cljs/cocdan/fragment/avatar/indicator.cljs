(ns cocdan.fragment.avatar.indicator
  (:require ["antd" :refer [Divider Modal]]
            [clojure.string :as s]
            [cocdan.core.settings :as settings]
            [cocdan.data.mixin.equipment :refer [get-equipments get-slots]]
            [cocdan.data.performer.core :refer [get-attr get-attr-max]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn- narrow-field-input-elem
  [{:keys [stage-id field-keyword current-value placeholder can-edit? val-fn style]
    :or {can-edit? false
         placeholder "请输入"
         style {}
         current-value ""}}]
  [:input.narrow-input
   {:defaultValue current-value
    :spellCheck false
    :placeholder placeholder
    :disabled (not can-edit?)
    :style style
    :onBlur (fn [x]
              (let [new-value (-> (-> x .-target .-value)
                                  (#(if val-fn (val-fn %) %)))]
                (when-not (= new-value current-value)
                  (rf/dispatch [:play/execute-transaction-props-to-remote-easy!
                                stage-id "update"
                                [[field-keyword (or current-value :unset) new-value]]]))))}])

(defn- center-colon
  []
  [:td {:style {:min-width "10px" :text-align "center"}} ":"])

(defn- attr-table-item
  [stage-id avatar attr-name is-kp?] 
  [:tr
   [:th attr-name]
   [center-colon]
   [:td
    {:style {:max-width "2em"}}
    ^{:key (str attr-name (get-attr avatar attr-name))}
    [narrow-field-input-elem {:stage-id stage-id
                              :field-keyword (keyword (str "avatars." (:id avatar) ".props.attrs." (s/lower-case attr-name)))
                              :current-value (get-attr avatar attr-name)
                              :placeholder "0"
                              :can-edit? is-kp?
                              :style {:max-width "2em"}}]]
   [:td
    {:style {:width "100%"}}
    (str " / " (get-attr-max avatar attr-name))]])

(defn- equipment-table
  [stage-id {avatar-id :id :as avatar}]
  (r/with-let
    [editor-open? (r/atom false)
     field-clicked (r/atom nil)
     equipment-edit (fn [slot-key equipment-val]
                      [:td [:input.narrow-input
                            {:style {:max-width "8em"}
                             :defaultValue equipment-val
                             :spellCheck false
                             :onBlur (fn [x]
                                       (let [new-value (-> x .-target .-value)]
                                         (when-not (= new-value equipment-val)
                                           (let [path-key (keyword (str "avatars." avatar-id ".props.equipments." (name slot-key)))
                                                 remove-before (if (empty? equipment-val) [] [path-key equipment-val :remove])
                                                 add-later (if (empty? new-value) [] [path-key :remove new-value])
                                                 final-ops (vec (filter seq [remove-before add-later]))]
                                             (when (seq final-ops)
                                               (rf/dispatch [:play/execute-transaction-props-to-remote-easy!
                                                             stage-id "update"
                                                             final-ops]))))))}]])] 
    [:div
     [:> Divider "物品栏"]
     [:table
      {:style {:width "100%"}}
      [:tbody 
       (apply concat
              (map (fn [[e-key e-val-list]]
                     (-> [(with-meta [:tr [:th {:on-click #(do (reset! editor-open? true)
                                                               (reset! field-clicked e-key))} (name e-key)]
                                      [equipment-edit e-key (first e-val-list)]] {:key (str e-key (first e-val-list))})]
                         (concat (map (fn [v]
                                        (with-meta
                                          [:tr [:th ""] [equipment-edit e-key (str v)]]
                                          {:key (str e-key (or v (js/Math.random)))}))
                                      (rest e-val-list)))))
                   (->> (get-slots avatar)
                        (map (fn [k]
                               (let [x (get-equipments avatar k false)]
                                 (if (empty? x)
                                   [k x] [k (conj x nil)]))))
                        (into {}))))]]
     [:> Modal
      {:title (str @field-clicked " 中的物品")
       :open @editor-open?
       :onOk #(reset! editor-open? false)
       :onCancel #(reset! editor-open? false)}
      (r/as-element [:p @field-clicked])]]))

(defn indicator
  [stage-id ctx avatar-id]
  (let [{:keys [name] :as avatar} (get-in ctx [:context/props :avatars (keyword (str avatar-id))])
        _refresh @(rf/subscribe [:partial-refresh/listen :play-room/avatar-indicator])]
    (when avatar 
      (let [is-kp? (settings/query-setting-value-by-key :game-play/is-kp)] 
        [:div
         [:> Divider "角色状态"]
         [:table
          {:style {:width "100%"}}
          [:tbody
           [:tr [:th "姓名"]
            (center-colon)
            [:td {:colSpan 2} [narrow-field-input-elem
                               {:stage-id stage-id
                                :field-keyword (keyword (str "avatars." avatar-id ".name"))
                                :current-value name
                                :placeholder "请输入角色姓名"
                                :can-edit? is-kp?}]]]
           [attr-table-item stage-id avatar "HP" is-kp?]
           [attr-table-item stage-id avatar "MP" is-kp?]
           [attr-table-item stage-id avatar "SAN" is-kp?]]]
         [equipment-table stage-id avatar]]))))

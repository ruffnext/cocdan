(ns cocdan.modal.avatar-edit 
  (:require [reagent.core :as r]
            [cocdan.data.core :as data-core]
            [re-frame.core :as rf]))

(defonce open? (r/atom false))
(defonce parameters (r/atom {}))

(defn launch
  [params]
  (reset! open? true)
  (reset! parameters params))

(defn modal
  []
  (when @open?
    (r/with-let
      [{{:keys [avatars substages] stage-id :id} :stage
        avatar-id :avatar-id} @parameters
       {:keys [name description substage]} ((keyword (str avatar-id)) avatars)
       avatar-edit (r/atom {:id (if avatar-id avatar-id (dec (apply min (map (fn [[_ {id :id}]] id) avatars))))
                            :name name
                            :substage substage
                            :description description})
       avatar-id-edit (if avatar-id avatar-id (dec (apply min (map (fn [[_ {id :id}]] id) avatars))))
       on-cancel-clicked (fn [_] (reset! open? false))
       on-save-clicked (fn [_]
                         (let [avatar-before {:avatars (if avatar-id
                                                         {(keyword (str avatar-id))
                                                          {:name name :description description :substage substage :id avatar-id}}
                                                         {})}
                               avatar-later {:avatars {(keyword (str (:id @avatar-edit))) @avatar-edit}}]
                           (rf/dispatch [:play/execute-transaction-props-to-remote-easy!
                                         stage-id "update"
                                         (data-core/diff' avatar-before avatar-later)])) 
                         (reset! open? false))]
      [:div.modal.is-active
       [:div.modal-background
        {:on-click on-cancel-clicked}]
       [:div.modal-card
        [:header.modal-card-head
         [:p.modal-card-title (if avatar-id (str "角色编辑 「" name "」") "新建 NPC")]
         [:button.delete {:aria-label "close"
                          :on-click on-cancel-clicked}]]
        [:section.modal-card-body
         [:div.field
          [:label.label "ID"]
          [:div.control>input.input
           {:disabled true
            :value avatar-id-edit}]]

         [:div.field
          [:label.label "姓名"]
          [:div.control>input.input
           {:placeholder "请输入您角色的名称"
            :defaultValue (or name "")
            :onBlur (fn [e] (swap! avatar-edit #(assoc % :name (-> e .-target .-value))))}]]

         [:div.field
          [:label.label "角色描述"]
          [:div.control>textarea.textarea
           {:placeholder "请输入您角色的描述"
            :defaultValue (or description "")
            :onBlur (fn [e] (swap! avatar-edit #(assoc % :description (-> e .-target .-value))))}]]

         [:div.field
          [:label.label "角色所在舞台"]
          [:select
           {:value substage
            :on-change (fn [e] (swap! avatar-edit #(assoc % :substage (-> e .-target .-value))))}
           (map (fn [[k v]]
                  (with-meta [:option {:value (cljs.core/name k)} (:name v)] {:key k})) substages)]]]

        [:footer.modal-card-foot
         [:button.button.is-success
          {:on-click on-save-clicked}
          "Save Changes"]
         [:button.button
          {:on-click on-cancel-clicked}
          "Cancel"]]]])))

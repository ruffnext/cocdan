(ns cocdan.modal.substage-edit 
  (:require ["antd" :refer [Transfer]]
            [cocdan.data.core :as data-core]
            [cocdan.data.mixin.territorial :refer [get-substage-id]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

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
      [{:keys [substage-id stage]} @parameters
       stage-id (:id stage)
       {:keys [name description] :as substage} (get-in stage [:substages (keyword (str substage-id))])
       default-target-keys (->> (filter (fn [[_k v]] (= (get-substage-id v) substage-id)) (:avatars stage))
                                (map #(-> % second :id)) set)
       target-keys (r/atom default-target-keys)
       all-avatar-items (map (fn [{:keys [id name] :as a}]
                               {:key id :title name :substage (get-substage-id a)})
                             (set (map second (:avatars stage))))
       substage-id-edit (r/atom substage-id)
       substage-name-edit (r/atom name)
       substage-description-edit (r/atom description)

       on-cancel-clicked (fn [_] (reset! open? false))
       on-save-clicked (fn [_]
                         (let [target-keys-set (set @target-keys)
                               substage-id @substage-id-edit
                               substage-id-key (keyword substage-id)
                               substage-before {:substages {substage-id-key (or substage {})}}
                               substage-after {:substages {substage-id-key {:id @substage-id-edit
                                                                            :name @substage-name-edit
                                                                            :description @substage-description-edit
                                                                            :adjacencies []}}}
                               diffs-substage (data-core/default-diff' substage-before substage-after)
                               diffs-avatars (reduce (fn [a {:keys [key substage]}]
                                                       (cond
                                                         (contains? default-target-keys key) a
                                                         (contains? target-keys-set key) (conj a [(keyword (str "avatars." key ".substage")) substage substage-id])
                                                         :else a)) [] all-avatar-items)
                               op (vec (concat diffs-substage diffs-avatars))]
                           (rf/dispatch [:play/execute-transaction-props-to-remote-easy! stage-id "update" op])
                           (reset! open? false)))]
      [:div.modal.is-active
       [:div.modal-background
        {:on-click on-cancel-clicked}]
       [:div.modal-card
        [:header.modal-card-head
         [:p.modal-card-title (if substage-id substage-id "新建子舞台")]
         [:button.delete {:aria-label "close"
                          :on-click on-cancel-clicked}]]
        [:section.modal-card-body
         [:div.field
          [:label.label "ID"]
          [:div.control>input.input
           {:disabled (some? substage-id)
            :onBlur #(reset! substage-id-edit (-> % .-target .-value))
            :defaultValue (or substage-id "")
            :placeholder "请设置子舞台的ID（惟一标识）"}]]

         [:div.field
          [:label.label "名称"]
          [:div.control>input.input
           {:placeholder "请输入子舞台名称"
            :defaultValue (or name "")
            :onBlur #(reset! substage-name-edit (-> % .-target .-value))}]]

         [:div.field
          [:label.label "子舞台描述"]
          [:div.control>textarea.textarea
           {:placeholder "请输入子舞台的描述"
            :defaultValue (or description "")
            :onBlur #(reset! substage-description-edit (-> % .-target .-value))}]]

         [:div.field
          [:label.label "转移舞台角色"]
          [:> Transfer
           {:dataSource all-avatar-items
            :titles ["其他舞台" "本舞台"]
            :oneWay true
            :targetKeys @target-keys
            :render (fn [x] (.-title x))
            :onChange #(reset! target-keys (js->clj %1))}]]]
        
        [:footer.modal-card-foot
         [:button.button.is-success
          {:on-click on-save-clicked}
          "Save Changes"]
         [:button.button
          {:on-click on-cancel-clicked}
          "Cancel"]]]])))

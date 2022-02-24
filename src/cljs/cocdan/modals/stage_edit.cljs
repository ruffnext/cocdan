(ns cocdan.modals.stage-edit
  (:require
   [reagent.core :as r]
   [cocdan.components.click-upload-img :refer [click-upload-img]]
   [cocdan.auxiliary :as aux]
   ["rich-markdown-editor" :refer (default) :rename {default RichMarkdownEditor}]
   [clojure.core.async :refer [go <!]]
   [cljs-http.client :as http]
   [re-frame.core :as rf]
   [cocdan.core.avatar :refer [posh-my-avatars]]
   [cocdan.db :as gdb]
   [cocdan.core.stage :refer [posh-stage-by-id]]))



(defonce active? (r/atom false))
(defonce title-editable (r/atom false))
(defonce stage (r/atom nil))
(defonce submit-status (r/atom "is-primary"))

(defn- edit-done
  []
  (let [stage @stage
        url (if (nil? (:id stage))
              "api/stage/create"
              (str "api/stage/s" (:id stage)))
        method (if (nil? (:id stage))
                 http/post
                 http/patch)]
    (reset! submit-status "is-loading")
    (go (let [res  (<! (method url {:json-params stage}))]
          (if (or (= 200 (:status res)) (= 201 (:status res)))
            (do
              (reset! submit-status "is-primary")
              (rf/dispatch [:event/refresh-my-avatars])
              (js/setTimeout #(reset! active? false) 1000))
            (reset! submit-status "is-danger"))))))

(defn- edit-cancel
  []
  (reset! active? false))

(aux/init-page
 {}
 {:event/modal-stage-edit-cancel edit-cancel
  :event/modal-stage-edit-active (fn [{:keys [db]} _ res]
                                   (when (not (nil? res))
                                     (reset! stage res)
                                     (reset! submit-status "is-primary"))
                                   (swap! active? not)
                                   {:db db})})

;; event/modal-stage-edit-active nil --> do not change current edit stage
;; event/modal-stage-edit-active {}  --> reset current edit stage to {}

(defn- generate-admin-options
  [avatar]
  [:option {:value (:id avatar)} (str (:name avatar) (if (nil? (:on_stage avatar))
                                                    ""
                                                    (str " @ " (let [stage (->> @(posh-stage-by-id gdb/db (:on_stage avatar))
                                                                                (gdb/pull-eid gdb/db))]
                                                                 (:title stage)))))])

(defn stage-edit
  []
  (when @active?
    [:div.modal {:class "is-active"}
     [:div.modal-background {:on-click edit-cancel}]
     [:div.modal-card
      [:header.modal-card-head
       [:input (merge (if @title-editable
                        {:class "input"}
                        {:class "input is-static" :readOnly true})
                      {:placeholder "Edit title here"
                       :onBlur #(reset! title-editable false)
                       :onFocus #(reset! title-editable true)
                       :on-change #(swap! stage (fn [s] (assoc s :title (-> % .-target .-value))))
                       :style {:margin-right "3em"}}
                      {:value (:title @stage)})]
       [:button.delete {:on-click edit-cancel}]]
      [:div.modal-card-image
       [click-upload-img {} (:banner @stage)
        {:on-uploaded #(swap! stage (fn [s] (assoc s :banner %)))}]]
      [:section.modal-card-body
       [:div.field
        [:label.label "Introduction"]
        [:div.content {:style {:padding-left "1.2em"}}
         [:> RichMarkdownEditor {:defaultValue (str (:introduction @stage))
                                 :on-change #(swap! stage (fn [s] (assoc s :introduction (%))))}]]]
       [:div.field
        [:hr]
        [:label.label "Settings"
         [:div.content
          [:div.field {:class "is-horizontal"}
           [:div {:class "field-label is-normal"}
            [:label.label "Code"]]
           [:div.field-body>div.field>p.control
            [:input {:class "input modal-stage-input"
                     :type "text"
                     :value (str (:code @stage))
                     :on-change #(swap! stage (fn [x] (assoc x :code (-> % .-target .-value))))
                     :placeholder "Generate by server"}]]]
          [:div.field {:class "is-horizontal"}
           [:div {:class "field-label is-normal"}
            [:label.label "Admin"]]
           [:div.field-body>div.field>div.control>div.select
            [:select
             {:on-change #(swap! stage (fn [x] (assoc x :owned_by (js/parseInt (-> % .-target .-value)))))
              :class "modal-stage-input"
              :defaultValue (:owned_by @stage)}
             [:option {:value 0} "create a new kp avatar"]
             (let [sid (:id @stage)
                   avatars (->> @(posh-my-avatars gdb/db)
                                (gdb/pull-eids gdb/db))]
               (doall (map-indexed (fn [i v] (when (= (:on_stage v) sid)
                                               (with-meta (generate-admin-options v) {:key (str "gaos" i)}))) avatars)))]]]]]]]
      [:footer.modal-card-foot
       [:button {:class (str "button " @submit-status)
                 :on-click edit-done} "Submit"]
       [:button.button {:on-click #(do
                                     (reset! active? false)
                                     (reset! stage {}))} "Cancel"]]
      [:button {:class "modal-close is-large" :on-click edit-cancel}]]]))


(ns cocdan.components.stage-simple
  (:require [markdown.core :refer [md->html]]
            [cocdan.auxiliary :as aux]
            [re-frame.core :as rf]
            [cljs-http.client :as http]))

(defn- handle-edit-modal-active
  [{:keys [db]} _ res _]
  {:db (assoc-in db [:stage-simple :edit-modal-active] res)})

(defn- handle-edit-modal-done
  [{:keys [db]} _ res _]
  (-> {:db db}
      (#(assoc-in % [:db :stage-simple :edit-modal] res))
      (#(if (nil? (:id res))
          (-> %
              (assoc-in [:http-request] {:url "/api/stage/create"
                                               :req-params {:json-params res}
                                               :method http/post
                                               :resp-event :event/stage-simple-update-done})
              (assoc-in [:db :stage-simple :edit-modal-submit-status] "is-loading"))
          (do
            (js/console.log "UPDATE modal")
            %)))))

(defn- handle-edit-modal
  [{:keys [db]} _ res _]
  {:db (update-in db [:stage-simple] assoc
                  :edit-modal-active false
                  :edit-modal res)})

(defn- handle-stage-update
  [{:keys [db]} _ res _]
  (let [stage (-> res :body)]
    (if (not= 201 (:status res))
      {:db (assoc-in db [:stage-simple :edit-modal-submit-status] "is-danger")}
      (do
        (js/setTimeout (fn [] (rf/dispatch [:event/stage-simple-edit-modal nil])) 1000)
        (rf/dispatch [:event/avatar-refresh])
        {:db (-> db
                 (assoc  :stages (aux/swap-filter-list-map! (:stages db) #(= (:id %) (:id stage)) (fn [_] stage)))
                 (assoc-in [:stage-simple :edit-modal-submit-status] "is-done"))}))))



(defn component-stage-simple
  [{id :id 
    title :title
    intro :introduction
    banner :banner
    owned_by :owned_by
    :as stage} card-type]
  (let [on-click (fn [_] (case card-type
                           "joined" (rf/dispatch [:event/modal-stage-info-active stage])
                           "controlled" (rf/dispatch [:event/modal-stage-edit-active stage])
                           (js/console.log (str "card type " card-type " not recognized"))))
        on-danger (fn [_] (case card-type
                            "controlled" (rf/dispatch [:event/modal-stage-delete-active stage])
                            nil))
        directed-by @(rf/subscribe [:subs/general-get-avatar-by-id owned_by])]
    [:div {:style {:margin "10px"}}
     [:div.card {:class "component-stage-simple"}
      [:div.card-image {:class "component-stage-simple-header"}
       [:figure {:class "image component-stage-simple-header" :on-click on-click}
        [:img {:src banner :style {:object-fit "cover"
                                   :height "100%"}}]]]
      [:div.card-content
       [:div.media
        [:div.media-left
         [:figure {:class "image is-48x48"}
          [:img {:src (:header directed-by)}]]]
        [:div.media-content
         [:p {:class "title is-4"} title]
         [:p {:class "subtitle is-6"} (str "Directed By " (:name directed-by))]]]
       [:div.content {:on-click on-click
                      :class "component-stage-simple-text"}
        [:div  {:dangerouslySetInnerHTML {:__html (md->html intro)}}]]]
      [:footer.card-footer
       [:a.card-footer-item {:class "has-text-primary"
                             :on-click #(rf/dispatch [:event/page-goto-stage id])} "Join"]
       [:a.card-footer-item {:class "has-text-info" :on-click on-click} (case card-type
                                                                          "joined" "Detail"
                                                                          "controlled" "Edit"
                                                                          "")]
       [:a.card-footer-item {:class "has-text-danger"
                             :on-click on-danger} (case card-type
                                                         "joined" "Quit"
                                                         "controlled" "Delete"
                                                         "")]]]]))

(defn component-edit
  [on-click]
  [:div {:style {:width "380px" :height "502px" :margin "10px"} :on-click on-click}
   [:div.card {:class "columns is-vcentered has-text-centered " :style {:height "100%" :margin "0px"}}
    [:div {:style {:width "100%"}}
     [:i {:class "fas fa-plus fa-7x" :style {:width "100%"}}]
     [:p {:class "subtitle is-3"} "Create One"]]]])

(defn component-search
  [on-click]
  [:div {:style {:width "380px" :height "502px" :margin "10px"} :on-click on-click}
   [:div.card {:class "columns is-vcentered has-text-centered " :style {:height "100%" :margin "0px"}}
    [:div {:style {:width "100%"}}
     [:i {:class "fas fa-search fa-7x" :style {:width "100%"}}]
     [:p {:class "subtitle is-3"} "Find One"]]]])

(aux/init-page
 {:subs/stage-simple-edit-modal-active #(->> % :stage-simple :edit-modal-active)
  :subs/edit-modal #(->> % :stage-simple :edit-modal)
  :subs/stages-joined (fn [db]
                        (let [my_avatars (set (map #(:id %) (filter #(= (:controlled_by %) (:id (:user db))) (:avatars db))))]
                          (filter #(not (contains? my_avatars (:owned_by %))) (:stages db))))
  :subs/stages-owned (fn [db]
                       (let [my_avatars (set (map #(:id %) (filter #(= (:controlled_by %) (:id (:user db))) (:avatars db))))]
                         (filter #(contains? my_avatars (:owned_by %)) (:stages db))))}
 {:event/stage-simple-edit-modal-active handle-edit-modal-active
  :event/stage-simple-edit-modal-done handle-edit-modal-done
  :event/stage-simple-edit-modal handle-edit-modal
  :event/stage-simple-update-done handle-stage-update})

(defn component-stages-list
  []
  (list
   ^{:key "mjs"} [:section.section
                  [:h1.title "我主持的舞台"]
                  [:div.columns {:class "is-multiline" :style {:margin-left 12}}
                   (doall (for [v @(rf/subscribe [:subs/stages-owned])]
                            (with-meta (component-stage-simple v "controlled") {:key (str (:id v))})))
                   (with-meta (component-edit #(rf/dispatch [:event/modal-stage-edit-active nil])) {:key "cssp1"})]]
   ^{:key "mms"} [:section.section
                  [:h1.title "我参加的舞台"]
                  [:div.columns {:class "is-multiline" :style {:margin-left 12}}
                   (doall (for [v @(rf/subscribe [:subs/stages-joined])]
                            (with-meta (component-stage-simple v "joined") {:key (str (:id v))})))
                   (with-meta (component-search #(rf/dispatch [:event/modal-find-stage-active true])) {:key "cssp"})]]))
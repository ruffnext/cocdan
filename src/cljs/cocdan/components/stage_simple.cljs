(ns cocdan.components.stage-simple
  (:require [markdown.core :refer [md->html]]
            [re-frame.core :as rf]
            [cocdan.db :as gdb]
            [cocdan.core.stage :refer [posh-stage-by-id default-stage]]
            [cocdan.core.avatar :refer [posh-avatar-by-id]]))

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
        directed-by (->> @(posh-avatar-by-id gdb/db owned_by)
                         (gdb/pull-eid gdb/db))]
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
                             :href (str "#/stage/" id)} "Join"]
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

(defn component-stages-list
  [avatar-eids]
  (let [avatars (for [avatar-eid avatar-eids] 
                  (gdb/pull-eid gdb/db avatar-eid))
        avatar-ids (set (map :id avatars))
        avatar-joined (set (filter #(not (nil? %)) (map :on_stage avatars)))
        stage-ids (set (reduce (fn [a {on_stage :on_stage}] (if on_stage (conj a on_stage) a)) [] avatars))
        stages (for [stage-id stage-ids]
                 (gdb/pull-eid gdb/db @(posh-stage-by-id gdb/db stage-id)))
        [stages-owned stages-joined _rest]
        (reduce (fn [a {owned_by :owned_by stage-id :id :as x}]
                  (if owned_by
                   (cond
                     (contains? avatar-ids owned_by) (assoc-in a [0] (conj (first a) x))
                     (contains? avatar-joined stage-id) (assoc-in a [1] (conj (second a) x))
                     :else (assoc-in a [2] (conj (nth a 2) x)))
                    a))
                [[] [] []] stages)]
    (list
     ^{:key "mjs"} [:section.section
                    [:h1.title "我主持的舞台"]
                    [:div.columns {:class "is-multiline" :style {:margin-left 12}}
                     (doall (for [v stages-owned]
                              (with-meta (component-stage-simple v "controlled") {:key (str (:id v))})))
                     (with-meta (component-edit #(rf/dispatch [:event/modal-stage-edit-active default-stage])) {:key "cssp1"})]]
     ^{:key "mms"} [:section.section
                    [:h1.title "我参加的舞台"]
                    [:div.columns {:class "is-multiline" :style {:margin-left 12}}
                     (doall (for [v stages-joined]
                              (with-meta (component-stage-simple v "joined") {:key (str (:id v))})))
                     (with-meta (component-search #(rf/dispatch [:event/modal-find-stage-active true])) {:key "cssp"})]])))
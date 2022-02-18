(ns cocdan.components.coc.stage-avatars 
  (:require 
   [re-frame.core :as rf]
   [cocdan.db :as gdb]
   [clojure.string :as str]
   [reagent.core :as r]))

(defn- avatar-item
  [{id :id :as avatar} my-avatars stage]
  (let [avatar-edit #(rf/dispatch [:event/modal-general-attr-editor-active
                                   :avatar [:attributes] avatar
                                   {:substage (sort (for [[{avatar-name :name} _v] (-> stage :attributes :substages)]
                                                      avatar-name))}])
        i-have-control? (gdb/posh-i-have-control? gdb/conn (:id stage))
        can-edit? (or i-have-control? (contains? (set (map :id my-avatars)) (:id avatar)))
        on-detail-edit (fn []
                         (cond
                           i-have-control?
                           (let [all-avatars (gdb/posh-avatar-by-stage-id gdb/conn (:id stage))]
                             (rf/dispatch [:event/modal-coc-avatar-edit-active all-avatars (:id avatar)]))
                           (contains? (set (map :id my-avatars)) (:id avatar))
                           (rf/dispatch [:event/modal-coc-avatar-edit-active my-avatars (:id avatar)])))
        unread-count (count (gdb/posh-unread-message-count gdb/conn id))]
    [:div {:style {:padding-left "6px"
                   :padding-bottom "3px"}} [:span.tag (:name avatar)]
     [:span.is-pulled-right ""]
     (when can-edit?
       [:span.tag.is-pulled-right.is-link
        {:style {:padding-right "12px" :padding-left "12px"}
         :on-click on-detail-edit}
        "EDIT"])
     [:span.is-pulled-right
      {:style (merge {:padding-right "12px" :padding-left "12px"} (when (not can-edit?)
                                                                    {:padding-right "3.9em"}))}
      [:span.tag
       {:on-click #(when i-have-control? (avatar-edit))}
       (if (nil? (-> avatar :attributes :substage))
        "not yet on stage"
        (-> stage
            :attributes
            :substages
            (#((keyword (-> avatar :attributes :substage str)) %))
            :name
            (str/split #">")
            ((fn [x] (take-last 1 x)))
            ; ((fn [x] (str/join ">" x)))
            ))]]
     (when (pos-int? unread-count)
       [:span.is-pulled-right.has-text-danger
        unread-count])]))

(defn stage-avatars
  [stage avatars my-avatars]
  (r/with-let [expand1? (r/atom true)
               expand2? (r/atom true)]
   [:div {:style {:margin "6px"}}
    [:p {:on-click #(swap! expand1? not)} "舞台上的角色"]
    (when @expand1?
      [:div
       (doall (for [avatar (sort-by #(-> % :attributes :substage) avatars)]
                (with-meta (avatar-item avatar my-avatars stage) {:key (str "sa-" (:id avatar))})))])
    [:hr {:style {:margin-top "0.5em" :margin-bottom "0.5em"}}]
    [:p {:on-click #(swap! expand2? not)} "我的角色"]
    (when @expand2?
      [:div
       {:style {:height "auto"
                :overflow-y "scroll"}}
       (doall (for [avatar (sort-by #(-> % :attributes :substage) my-avatars)]
                (with-meta (avatar-item avatar my-avatars stage) {:key (str "sa2-" (:id avatar))})))])]))
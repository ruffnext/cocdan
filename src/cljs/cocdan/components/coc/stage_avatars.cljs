(ns cocdan.components.coc.stage-avatars 
  (:require 
   [re-frame.core :as rf]
   [cocdan.db :as gdb]))

(defn- avatar-item
  [{id :id :as avatar} stage]
  (let [avatar-edit #(rf/dispatch [:event/modal-general-attr-editor-active
                                   :avatar [:attributes] avatar
                                   {:substage (sort (for [[k _v] (-> stage
                                                                     :attributes
                                                                     :substages)]
                                                      (name k)))}])
        on-detail-edit (fn []
                         (let [all-avatars (gdb/posh-avatar-by-stage-id gdb/conn (:id stage))]
                           (rf/dispatch [:event/modal-coc-avatar-edit-active all-avatars (:id avatar)])))
        unread-count (count (gdb/posh-unread-message-count gdb/conn id))]
    [:div {:style {:padding-left "6px"
                   :padding-bottom "3px"}} (:name avatar)
     [:span.is-pulled-right ""]
     [:p.is-pulled-right
      {:style {:padding-right "12px" :padding-left "12px"}
       :on-click on-detail-edit}
      "EDIT"]
     [:span.is-pulled-right
      {:style {:padding-right "12px" :padding-left "12px"}
       :on-click avatar-edit}
      (if (nil? (-> avatar :attributes :substage))
        "not yet on stage"
        (-> stage
            :attributes
            :substages
            (#((keyword (-> avatar :attributes :substage str)) %))
            :name))]
     (when (pos-int? unread-count)
       [:span.is-pulled-right.has-text-danger
        unread-count])]))

(defn stage-avatars
  [stage avatars]
  [:div {:style {:margin "6px"}}
   [:p "舞台上的角色"]
   (doall (for [avatar avatars]
            (with-meta (avatar-item avatar stage) {:key (str "sa-" (:id avatar))})))])
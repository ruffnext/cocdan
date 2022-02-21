(ns cocdan.pages.stage
  (:require
   [cocdan.auxiliary :as gaux]
   [cocdan.components.coc.general-status :refer [general-status]]
   [cocdan.components.coc.stage-avatars :refer [stage-avatars]]
   [cocdan.components.chatting-log :refer [chatting-log]]
   [cocdan.components.chatting-input :refer [chatting-input]]
   [cocdan.core.avatar :refer [posh-current-use-avatar-eid posh-avatars-by-stage-id posh-my-avatars]]
   [cocdan.core.stage :refer [posh-stage-by-id]]
   [cocdan.modals.network-indicator :refer [network-indicator]]
   [cocdan.db :as gdb]
   [re-frame.core :as rf]))

(defn- init-stage
  [_ _ stage-id]
  (let [res @(posh-current-use-avatar-eid gdb/conn (js/parseInt stage-id))]
    (if (nil? res)
      {:fx/chat-new-stage {:stage-id stage-id}
       :fx/stage-refresh-avatars {:stage-id stage-id}
       :fx/stage-refresh {:stage-id stage-id}}
      {})))

(defn- goto-stage
  [_ _ stage-id]
  (rf/dispatch [:common/navigate! :stage {:id stage-id}])
  (rf/dispatch [:event/page-init-stage stage-id])
  {})

(gaux/init-page
 {}
 {:event/page-goto-stage goto-stage
  :event/page-init-stage init-stage})

(defn page
  [{stage-id :id}]

  (let [stage-id (js/parseInt stage-id)
        stage (->> @(posh-stage-by-id gdb/conn stage-id) (gdb/pull-eid gdb/conn))
        avatars-on-stage (->> @(posh-avatars-by-stage-id gdb/conn stage-id) (gdb/pull-eids gdb/conn))
        my-avatars (->> @(posh-my-avatars gdb/conn) (gdb/pull-eids gdb/conn)
                        (filter #(= (-> % :on_stage) stage-id)))
        current-use-avatar (->> @(posh-current-use-avatar-eid gdb/conn stage-id) (gdb/pull-eid gdb/conn))]
    (when (not (nil? current-use-avatar))
      [:div.container
       [:div.card.columns.stage-container
        [:div.column.sketch.stage-left-panel
         [:div.sketch
          (general-status stage current-use-avatar)]
         [:div.sketch
          (stage-avatars stage (-> (filter #(= (-> % :attributes :substage)
                                               (-> current-use-avatar :attributes :substage))
                                           avatars-on-stage)
                                   flatten set vec)
                         my-avatars)]]
        [:div.column.sketch.stage-right-panel
         [:div.stage-log-container
          [chatting-log stage (:id current-use-avatar) my-avatars]]
         [:div.stage-input-container
          (chatting-input stage-id (filter #(and (=
                                                  (-> % :attributes :substage)
                                                  (-> current-use-avatar :attributes :substage))
                                                 (not= (:id %) (:id current-use-avatar)))
                                           avatars-on-stage) my-avatars current-use-avatar)]]]
       (network-indicator (:channel stage))])))


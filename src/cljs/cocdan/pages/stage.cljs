(ns cocdan.pages.stage
  (:require
   [cocdan.auxiliary :as gaux]
   [cocdan.components.coc.general-status :refer [general-status]]
   [cocdan.components.coc.stage-avatars :refer [stage-avatars]]
   [cocdan.components.chatting-log :refer [chatting-log]]
   [cocdan.components.chatting-input :refer [chatting-input]]
   [cocdan.core.avatar :refer [posh-current-use-avatar-eid]]
   [cocdan.modals.network-indicator :refer [network-indicator]]
   [cocdan.core.chat :refer [init-stage-ws!]]
   [cocdan.db :as gdb]
   [re-frame.core :as rf]
   [cocdan.core.log :refer [posh-stage-latest-ctx-eid]]
   [cocdan.core.user :refer [posh-my-eid]]
   [cocdan.core.stage :refer [posh-stage-by-id]]
   [clojure.core.async :refer [go]]))

(defn- init-stage
  [_ _ stage-id]
  (let [res @(posh-current-use-avatar-eid gdb/db (js/parseInt stage-id))]
    (if (nil? res)
      {:fx/chat-new-stage {:stage-id stage-id}}
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
        current-use-avatar
        (->> @(posh-current-use-avatar-eid gdb/db stage-id)
             (gdb/pull-eid gdb/db))]
    (if current-use-avatar
      (let [ctx-eid (posh-stage-latest-ctx-eid gdb/db stage-id)
            {{avatars-on-stage :avatars
              stage :stage} :fact} (gdb/pull-eid gdb/db ctx-eid)
            my-info (->> @(posh-my-eid gdb/db)
                         (gdb/pull-eid gdb/db))
            my-avatars (filter #(= (:controlled_by %) (:id my-info)) avatars-on-stage)]
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
           (network-indicator (:channel (->> @(posh-stage-by-id gdb/db stage-id)
                                             (gdb/pull-eid gdb/db))))]))
      (go
        (init-stage-ws! {:stage-id stage-id})))))


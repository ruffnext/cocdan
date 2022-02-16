(ns cocdan.pages.stage
  (:require
   [cocdan.auxiliary :as gaux]
   [cocdan.components.coc.general-status :refer [general-status]]
   [cocdan.components.coc.stage-avatars :refer [stage-avatars]]
   [cocdan.components.chatting-log :refer [chatting-log]]
   [cocdan.components.chatting-input :refer [chatting-input]]
   [cocdan.db :as gdb]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [posh.reagent :as p]))

(defn- init-stage
  [_ _ stage-id]
  (let [res (gdb/posh-current-use-avatar-id gdb/conn (js/parseInt stage-id))]
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

(defn- get-msgs
  [current-use-avatar]
  (let [ids (->> @(p/q '[:find ?id
                         :in $ ?current-use-avatar-id
                         :where [?id :message/receiver ?current-use-avatar-id]]
                       gdb/conn
                       current-use-avatar)
                 (reduce into []))
        msgs @(p/pull-many gdb/conn '[*] ids)]
    (->> (gdb/remove-db-perfix msgs)
         (sort-by :time)
         reverse
         (take 40)
         reverse)))

(comment
  (let [avatars-on-stage (gdb/posh-avatar-by-stage-id gdb/conn 2)
        my-avatars (gdb/posh-my-avatars gdb/conn)]
     (count (set (flatten (conj avatars-on-stage my-avatars)))))
  (gdb/posh-current-use-avatar-id gdb/conn 1)
  
  )

(defn page
  [{stage-id :id}]
  (r/with-let
    [_init (rf/dispatch [:event/page-init-stage (js/parseInt stage-id)])]
    (let [stage-id (js/parseInt stage-id)
          stage (gdb/posh-stage-by-id gdb/conn stage-id)
          avatars-on-stage (gdb/posh-avatar-by-stage-id gdb/conn stage-id)
          my-avatars (gdb/posh-my-avatars gdb/conn)
          current-use-avatar (gdb/posh-current-use-avatar gdb/conn stage-id)]
      (when (not (nil? current-use-avatar))
        [:div.container
         [:div.card {:style {:height "78vh"
                             :margin-top "2em"
                             :margin-bottom "2em"}
                     :class "columns"}
          [:div.column {:class "sketch" :style {:min-width "19em"
                                                :max-width "19em"
                                                :height "100%"}}
           [:div {:class "sketch"}
            (general-status stage current-use-avatar)]
           [:div {:class "sketch"
                  :style {:margin-top "1em"}}
            (stage-avatars stage (-> (filter #(= (-> %
                                                     :attributes
                                                     :substage)
                                                 (-> current-use-avatar
                                                     :attributes
                                                     :substage))
                                             avatars-on-stage)
                                     (conj my-avatars)
                                     flatten
                                     set
                                     vec))]]
          [:div.column {:class "sketch" :style {:height "100%"}}
           [:div
            {:style {:overflow-y "scroll"
                     :height "68%"}}
            [chatting-log (get-msgs (:id current-use-avatar)) my-avatars]]
           [:div {:style {:height "20%"
                          :margin-top "calc(5%)"}}
            (chatting-input stage-id my-avatars current-use-avatar)]]]]))))


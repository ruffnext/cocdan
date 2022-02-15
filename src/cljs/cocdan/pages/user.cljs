(ns cocdan.pages.user
  (:require
   [cocdan.auxiliary :as aux]
   [cocdan.components.avatar-simple :refer [component-avatars-list component-avatar-simple]]
   [cocdan.components.stage-simple :refer [component-stages-list]]
   [re-frame.core :as rf]
   [cljs-http.client :as http]
   [reitit.frontend.easy :as rfe]
   [re-posh.core :as rp]))

(defn- try-session-login
  [{:keys [db]} _]
  (-> {:db db}
      (#(assoc % :http-request {:url "/api/user/whoami"
                                      :method http/get
                                      :resp-event :event/user-initialize}))))

(defn- user-init
  [{:keys [db]} _ res _]
  (let [user (-> res :body :user)]
    (rp/dispatch [:rpevent/upsert :my-info user])
    (if (nil? user)
      {:db db}
      {:db (assoc db :user user)
       :http-request [{:url "/api/user/avatar/list"
                             :method http/get
                             :resp-event :event/avatar-initialize}]})))

(defn- refresh-avatar-list
  [_ & __]
  {:http-request [{:url "/api/user/avatar/list"
                         :method http/get
                         :resp-event :event/avatar-initialize}]})

(defn- refresh-avatar-list-done
  [{:keys [db]} _ res _]
  (let [avatars (-> res :body)]
    (if (or (not= (:status res) 200) (empty? avatars))
      {:db db}
      {:db (assoc db :avatars avatars)})))


(defn- query-stages-from-avatar
  [avatars]
  (set (remove nil? (reduce (fn [a x]
                              (conj a (:on_stage x))) [] avatars))))

(defn- avatar-init
  [{:keys [db]} _ res _]
  (let [avatars (-> res :body)]
    (rp/dispatch [:rpevent/upsert :avatar avatars])
    (if (or (not= (:status res) 200) (empty? avatars))
      {:db db}
      {:db (assoc db :avatars avatars)
       :http-request (map (fn [x] {:url (str "api/stage/s" x)
                                         :method http/get
                                         :resp-event :event/stage-initialize}) (query-stages-from-avatar avatars))})))

(defn- stage-init
  [{:keys [db]} _ res _]
  (let [stage (-> res :body)]
    (if (or (not= (:status res) 200) (empty? stage))
      {:db db}
      {:db (assoc db :stages (aux/swap-filter-list-map! (:stages db) #(= (:id %) (:id stage)) (fn [_] stage)))})))

(aux/init-page
 {}
 {:event/try-session-login try-session-login
  :event/user-initialize user-init
  :event/avatar-initialize avatar-init
  :event/stage-initialize stage-init
  :event/avatar-refresh refresh-avatar-list
  :event/avatar-refresh-done refresh-avatar-list-done})

(defn component-user-info-detail
  [user]
  [:div
   [:h1.title "My Info"]
   [:div {:class "card-content"}
    [:div.media
     [:div.media-left
      [:figure {:class "image is-128x128"}
       [:img {:src "/img/header.png"}]]]
     [:div.media-content
      [:h2.title (:name user)]]]]])

(defn user-page
  [user]
  (let [my-avatars @(rf/subscribe [:subs/general-get-avatars-by-user-id (:id user)])]
    [:div.container
     [:section.section
      [:div.columns {:class "is-horizontal"}
       [:div.column {:class "is-two-third"}
        (component-user-info-detail user)]
       [:div.column {:class "is-one-third" :style {:margin-right "3em"}}
        [:h3.title {:class "is-4"} "Legendary Investigator"]
        (component-avatar-simple {})]]]
     (component-stages-list)
     [:section.section
      [:h1.title "我的角色"]
      [:div.columns {:class "is-multiline" :style {:margin-left 12}}
       (component-avatars-list my-avatars)]]]))

(defn page
  []
  (let [user @(rf/subscribe [:user])]
    (if (empty? user)
      (rfe/push-state :login)
      (user-page user))))
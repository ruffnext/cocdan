(ns cocdan.avatars.services
  (:require [cocdan.schema.core :as schema]
            [cocdan.users.core :as users]
            [cocdan.users.auxiliary :as usersaux]
            [cocdan.avatars.auxiliary :as aux]
            [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [cocdan.ws.db :as ws-db]
            [clojure.spec.alpha :as s]
            [cocdan.auxiliary :as gaux]))

(defn create!
  [{{{:keys [name on_stage] :as avatar} :body} :parameters session :session}]
  {:pre [(not (string/blank? name))]}
  (m/mlet [user (users/login? session)
           res (aux/create-avatar! (assoc avatar :controlled_by (:id user) :on_stage on_stage))]
          (either/right {:status 201 :body res})))



(defn delete!
  [{{{avatarId :id} :path} :parameters session :session}]
  (m/mlet [user (users/login? session)
           avatar (aux/get-avatar-by-id? avatarId)
           _ (cond
               (= (:id user) (:controlled_by avatar)) (either/right "")
               :else (either/left (format "avatar %d is not controlled by you" avatarId)))
           res (aux/delete-avatar! avatarId)]
          (m/return {:status 204 :body {:msg res}})))

(defn get'
  [{{{id :id} :path} :parameters session :session}]
  (m/mlet [_ (users/login? session)
           res (aux/get-avatar-by-id? id)]
          (m/return {:body res})))

(defn transfer!
  [{session :session  {{id :id} :path
                       {controlled_by :controlled_by :as newAvatar} :body} :parameters}]
  {:per [(int? id)]}
  (m/mlet [user (users/login? session)
           avatar (aux/get-avatar-by-id? id)
           _ (cond
               (not= (:controlled_by avatar) (:id user)) (either/left (format "you don't have permission to modifiy avatar %d" id))
               :else (either/right ""))
           new-controller (if (nil? controlled_by)
                            (either/right user)
                            (usersaux/get-by-id? controlled_by))
           res (aux/transfer-avatar! (->
                                      (gaux/flatten-map avatar)
                                      (merge (gaux/flatten-map newAvatar))
                                      (gaux/reconstruct-map-kvs)
                                      (assoc :controlled_by (:id new-controller))))]
          (m/return {:body res})))


(s/def ::name string?)
(s/def ::controlled_by int?)
(s/def ::attributes map?)

(def service-routes
  ["/avatar"
   {:swagger {:tags ["avatar"]}}
   ["/create"
    {:post {:summary "create a avatar"
            :parameters {:body {:name string?}}
            :responses {201 {:schema schema/SchemaAvatar}
                        401 {:schema schema/SchemaError}}
            :handler #(-> %
                          create!
                          schema/middleware-either-api)}}]
   ["/a:id"
    {:delete {:summary "delete a avatar owned by you"
              :parameters {:path {:id int?}}
              :responses {204 {}
                          401 {:schema schema/SchemaError}}
              :handler #(-> %
                            delete!
                            schema/middleware-either-api)}
     :get {:summary "get specified avatars owned and controlled by you"
           :parameters {:path {:id int?}}
           :responses {200 {:body [schema/SchemaAvatar]
                            :description "User"}}
           :handler #(-> %
                         get'
                         schema/middleware-either-api)}
     :patch {:summary "transfer control permission to another user"
             :parameters {:path {:id int?}
                          :body (s/keys :opt-un [::controlled_by ::name ::attributes])}
             :responses {200 {:schema schema/SchemaAvatar}
                         401 {:schema schema/SchemaError}}
             :handler #(-> %
                           transfer!
                           schema/middleware-either-api
                           (ws-db/middleware-ws-update :avatar))}}]])
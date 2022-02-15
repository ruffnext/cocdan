(ns cocdan.users.services
  (:require [cocdan.schema.core :as schema]
            [cocdan.users.auxiliary :as aux]
            [cocdan.users.core :refer [login?]]
            [ring.util.response :refer [response]]
            [cats.monad.either :as either]
            [cats.core :as m]
            [clojure.tools.logging :as log]
            [cocdan.avatars.auxiliary :as avatarsaux]))

(defn login
  [{{body :body} :parameters session :session}]
  (m/mlet [_ (schema/invert-left (login? session)
                                 (fn [_] (either/left {:error "You have already logined!" :status 400})))
           user (aux/get-user-by-email? body)]
          (either/right (-> (response {:user user})
                            (assoc :session (assoc session :user user))))))

(defn logout
  [{session :session}]
  (m/mlet [_ (login? session)]
          (either/right (-> (response {:msg (str "User removed")})
                            (assoc :session nil)))))

(defn register
  [{{body :body} :parameters}]
  (m/mlet [_ (schema/invert-left  (aux/get-user-by-email? body)
                                  (fn [_] (either/left {:error "User already exists!" :status 400})))
           _ (aux/register! body)]
          (m/return {:status 200 :body {:msg "User created!"}})))

(defn whoami
  [{session :session}]
  (m/mlet [user (login? session)]
          (either/right (response {:user user}))))

(defn- list-avatar-by-user-id
  [{session :session}]
  (m/mlet [user (login? session)
           avatars (avatarsaux/list-avatars-by-user? (:id user))]
          (m/return {:body avatars})))

(defn- get-user-by-id?
  [{{{id :id} :path} :parameters session :session}]
  (m/mlet [_ (login? session)
           user (aux/get-by-id? id)]
          (m/return {:body user})))

(defn cookie-test
  [parameters]
  #_{:clj-kondo/ignore [:redundant-do]}
  (do
    (log/info (str (select-keys parameters [:cookies])))
    {:status 200 :body {:msg "OK"}}))

(def service-routes
  ["/user"
   {:swagger {:tags ["user"]}}
   ["/register"
    {:post {:summary "regist a user"
            :parameters {:body {:name string? :email string?}}
            :responses {200 {:schema schema/SchemaUser
                             :description "User"}
                        400 {:schema schema/SchemaError}}
            :handler #(-> %
                          register
                          schema/middleware-either-api)}}]
   ["/login"
    {:post {:summary "login"
            :parameters {:body {:name string? :email string?}}
            :responses {200 {:schema schema/SchemaUser}
                        400 {:schema schema/SchemaError}}
            :handler #(-> %
                          login
                          schema/middleware-either-api)}}]
   ["/logout"
    {:delete {:summary "logout"
              :responses {204 {:body {:msg string?}}
                          401 {:schema schema/SchemaError}}
              :handler #(-> %
                            logout
                            schema/middleware-either-api)}}]
   ["/whoami"
    {:get {:summary "get self info"
           :response {200 {:schema schema/SchemaUser}
                      401 {:schema schema/SchemaError}}
           :handler #(-> %
                         whoami
                         schema/middleware-either-api)}}]
   ["/u:id"
    {:get {:summary "get user info"
           :parameters {:path {:id int?}}
           :responses {200 {:schema schema/SchemaUser}}
           :handler #(-> %
                         get-user-by-id?
                         schema/middleware-either-api)}}]
   ["/avatar/list"
    {:get {:summary "list all avatars owned and controlled by you"
           :parameters {}
           :responses {200 {:schema [schema/SchemaAvatar]}
                       204 {}
                       401 {:schema schema/SchemaError}}
           :handler #(-> %
                         list-avatar-by-user-id
                         schema/middleware-either-api)}}]
   ["/cookie"
    {:get {:summary "test"
           :handler cookie-test}}]])
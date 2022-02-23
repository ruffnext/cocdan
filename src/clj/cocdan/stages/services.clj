(ns cocdan.stages.services
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [cocdan.db.core :as db]
            [cocdan.stages.auxiliary :as aux]
            [cocdan.avatars.auxiliary :as avatarsaux]
            [cocdan.auxiliary :as gaux]
            [cocdan.users.core :as users]
            [cocdan.schema.core :as schema]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [cocdan.middleware.ws :refer [middleware-ws-update]]
            [cocdan.shell.db :refer [query-stage-action?]]
            [cocdan.ws.db :refer [remove-db-perfix]]))

(defn- create!
  [{{{_ :title
      owned_by :owned_by
      __ :banner
      ___ :introduction
      :as stage} :body} :parameters session :session}]
  (m/mlet [user (users/login? session)
           avatarId (cond
                      (nil? owned_by) (either/try-either (db/create-avatar! {:name "KP" :controlled_by (:id user) :on_stage nil}))
                      :else (either/right {:id owned_by}))
           avatar (avatarsaux/get-avatar-by-id? (:id avatarId))
           _ (cond
               (not= (:controlled_by avatar) (:id user)) (either/left (format "you don't have permission to control avatar %d" avatarId))
               (not (nil? (:on_stage avatar))) (either/left (format "the avatar is already on another stage %d" (:on_stage avatar)))
               :else (either/right ""))
           stage (aux/create-stage! {:stage (dissoc stage :owned_by) :owned_by avatar})]
          (m/return {:body stage :status 201})))

(defn- get?
  [{{sid :id} :path-params session :session}]
  (m/mlet [_ (users/login? session)
           stage (aux/get-by-id? sid)]
          (m/return {:body stage})))

(defn- delete?
  [{{sid :id} :path-params session :session}]
  (m/mlet [user (users/login? session)
           stage (aux/get-by-id? sid)
           avatars (avatarsaux/list-avatars-by-user? (:id user))
           KP (aux/check-control-permission avatars stage)
           stage-avatars (avatarsaux/list-avatars-by-stage? sid)
           _ (m/foldm (fn [_ avatar] (avatarsaux/transfer-avatar! (assoc avatar :on_stage nil))) "" stage-avatars)
           _ (aux/delete-stage (:id stage))
           _ (avatarsaux/delete-avatar! (:id KP))]
          (m/return {:body {} :status 204})))

(defn- patch!
  [{{sid :id} :path-params stage_patch :body-params session :session}]
  (m/mlet [sid (cond
                 (int? sid) (either/right sid)
                 (string? sid) (either/try-either (Integer/parseInt sid)))
           user (users/login? session)
           stage (aux/get-by-id? sid)
           avatars (avatarsaux/list-avatars-by-user? (:id user))
           _KP (aux/check-control-permission avatars stage)
           stage-patch-modified (cond
                                  (or (nil? (:owned_by stage_patch)) (= (:owned_by stage_patch) (:owned_by stage)))  (either/right stage_patch)
                                  (= 0 (:owned_by stage_patch)) (m/mlet [new-avatar (either/try-either (db/create-avatar! {:name "KP" :controlled_by (:id user) :on_stage sid}))]
                                                                        (either/right (assoc stage_patch :owned_by (:id new-avatar))))
                                  :else (m/mlet [avatar (avatarsaux/get-avatar-by-id? (:owned_by stage_patch))]
                                                (if (or (nil? (:on_stage avatar)) (= (:on_stage avatar) sid))
                                                  (m/mlet [_ (avatarsaux/transfer-avatar! (assoc avatar :on_stage sid))]
                                                          (m/return stage_patch))
                                                  (either/left (format "avatar %d is on another stage %d , can't control this stage (id = %d)" (:id avatar) (:on_stage avatar) sid)))))
           res (->
                (gaux/flatten-map stage)
                (merge (gaux/flatten-map (dissoc stage-patch-modified :id)))
                (gaux/reconstruct-map-kvs)
                (#(either/right (assoc % :attributes (json/write-str (:attributes %))))))
           _ (either/try-either (db/general-transfer {:table "stages"
                                                      :updates (dissoc res :id)
                                                      :id sid}))
           return_res (aux/get-by-id? sid)]
          (m/return {:body return_res})))


(defn- make-invite!
  [{{{sid :id} :path} :parameters session :session}]
  (m/mlet [user (users/login? session)
           stage (aux/get-by-id? sid)
           avatars (avatarsaux/list-avatars-by-user? (:id user))
           _ (aux/check-control-permission avatars stage)]))

(defn- get-by-code?
  [{{{code :code} :query} :parameters session :session}]
  (m/mlet [_ (users/login? session)
           stage (aux/get-by-code? code)]
          (m/return {:body stage})))

(defn- list-avatars
  [{{{code :code
      avatar-id :avatar-id} :query
     {stage-id :id} :path} :parameters session :session}]
  (m/mlet [user (users/login? session)
           stage (cond
                   (not (nil? code)) (aux/get-by-code? code)
                   (not (nil? avatar-id)) (m/mlet [avatar (avatarsaux/get-avatar-by-id? avatar-id)]
                                                  (cond
                                                    (not= (:controlled_by avatar) (:id user)) (either/left (format "this avatar (id = %d) is not owned by you" avatar-id))
                                                    (not= (:on_stage avatar) stage-id) (either/left (format "this avatar (id = %d) is not on stage (id = %d)" avatar-id stage-id))
                                                    :else (aux/get-by-id? stage-id)))
                   :else (either/left "code or avatar-id, one of the two must be provided!"))
           avatars (avatarsaux/list-avatars-by-stage? (:id stage))]
          (m/return {:body avatars})))

(defn- join-by-code!
  [{{{code :code} :query {avatar :avatar} :body} :parameters session :session}]
  (m/mlet [_ (users/login? session)
           stage (aux/get-by-code? code)
           avatar (avatarsaux/get-avatar-by-id? avatar)
           avatar' (avatarsaux/transfer-avatar! (assoc avatar :on_stage (:id stage)))]
          (m/return {:body avatar'})))

(defn- query-history-actions?
  [{{{stage-id :id} :path
     {orders :orders} :query} :parameters session :session}]
  (m/mlet [_ (users/login? session)]
          (m/return {:status 200
                     :body (->> (if (seq orders)
                                  orders
                                  [(Integer/parseInt orders)])
                                (map #(->
                                       (query-stage-action? stage-id %)
                                       remove-db-perfix))
                                (filter #(seq %)))})))


(s/def ::code string?)
(s/def ::avatar-id int?)

(def service-routes
  ["/stage"
   {:swagger {:tags ["stage"]}}
   ["/create"
    {:post {:summary "create a stage"
            :parameters {:body {:title string?
                                :banner string?
                                :introduction string?}}
            :responses {201 {:schema schema/SchemaStage}}
            :handler #(-> %
                          create!
                          schema/middleware-either-api)}}]
   ["/s:id"
    {:get {:summary "query a stage"
           :parameters {:path {:id string?}}
           :responses {200 {:body {:msg string?}}}
           :handler #(-> %
                         get?
                         schema/middleware-either-api)}
     :delete {:summary "delete a stage controlled by you"
              :parameters {:path {:id int?}}
              :responses {204 {}
                          401 {:schema schema/SchemaError}}
              :handler #(-> %
                            delete?
                            schema/middleware-either-api)}
     :patch {:summary "update a stage controlled by you"
             :parameters {:path {:id int?}
                          :body any?}
             :responses {200 {:schema schema/SchemaStage}
                         401 {:schema schema/SchemaError}}
             :handler #(-> %
                           patch!
                           schema/middleware-either-api
                           (middleware-ws-update :stage))}}]
   ["/get-by-code"
    {:get {:summary "get stage info by code"
           :parameters {:query {:code string?}}
           :responses {200 {:schema schema/SchemaStage}}
           :handler #(-> %
                         get-by-code?
                         schema/middleware-either-api)}}]
   ["/s:id/list/avatar"
    {:get {:summary "list a stage's avatars"
           :parameters {:query (s/keys :opt-un [::code ::avatar-id])
                        :path {:id int?}}
           :responses {200 {:schema [schema/SchemaAvatar]}}
           :handler #(-> %
                         list-avatars
                         schema/middleware-either-api)}}]
   ["/s:id/history-actions"
    {:get {:summary "query history actions"
           :parameters {:path {:id int?}
                        :query {:orders [int?]}}
           :handler #(-> %
                         query-history-actions?
                         schema/middleware-either-api)}}]
   ["/s:id/make-invite"
    {:post {:summary "generate a invite url"
            :parameters {:path {:id string?}}
            :responses {200 {:body {:msg string?}}}
            :handler #(-> %
                          make-invite!
                          schema/middleware-either-api)}}]
   ["/join-by-code"
    {:post {:summary "join a stage by code"
            :parameters {:query {:code string?}
                         :body {:avatar int?}}
            :responses {200 {:schema schema/SchemaAvatar}}
            :handler #(-> %
                          join-by-code!
                          schema/middleware-either-api
                          (middleware-ws-update :avatar))}}]])



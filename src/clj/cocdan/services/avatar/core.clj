(ns cocdan.services.avatar.core
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [cocdan.db.core :as db]
            [taoensso.nippy :as nippy]
            [cocdan.db.monad-db :as monad-db]))

(defn- check-stage
  [stage]
  (if (> stage 0) (monad-db/get-stage-by-id stage) (either/right)))

(defn create-avatar!
  [{:keys [name image description stage substage props]} access-user-id]
  (m/mlet [_check-stage (check-stage stage)
           res (either/try-either
                (db/create-avatar!
                 {:name name
                  :image image
                  :description description
                  :stage stage
                  :substage substage
                  :controlled_by access-user-id
                  :props (nippy/freeze (or props {}))}))]
          (let [inserted-id (second (first res))]
            (monad-db/get-avatar-by-id inserted-id))))

(defn get-avatar-by-id
  [avatar-id]
  (monad-db/get-avatar-by-id avatar-id))

(defn update-avatar-by-id
  [avatar-id {:keys [name image description stage substage props controlled_by]} access-user-id]
  (m/mlet
   [{controller :controlled_by} (monad-db/get-avatar-by-id avatar-id)
    _check-user-access (if (= controller access-user-id)
                         (either/right) (either/left (str "用户 id=" access-user-id " 无权修改角色 id=" avatar-id)))
    _check-stage (check-stage stage)
    _check-new-controller (if controlled_by (monad-db/get-user-by-id controlled_by) (either/right))
    _work (either/try-either (db/general-updater
                              {:id avatar-id
                               :updates (->> {:name name
                                              :image image
                                              :description description
                                              :stage stage
                                              :substage substage
                                              :controlled_by controlled_by
                                              :props (if props (nippy/freeze props) nil)}
                                             (filter (fn [[_k v]] v))
                                             (into {}))
                               :table "avatars"}))]
   (monad-db/get-avatar-by-id avatar-id)))

(defn delete-avatar!
  [avatar-id access-user-id]
  (m/mlet 
   [{:keys [controlled_by]} (monad-db/get-avatar-by-id avatar-id)
    _check-user-access (if (= access-user-id controlled_by) (either/right) (either/left (str "用户 id=" access-user-id " 无权删除橘色 id=" avatar-id)))]
   (either/try-either
    (db/general-delete {:id avatar-id
                        :table "avatars"}))))

(defn get-avatars-by-user-id
  [user-id]
  (m/mlet
   [_user (monad-db/get-user-by-id user-id)]
   (monad-db/get-avatars-by-user-id user-id)))

(defn get-avatars-by-stage-id
  [stage-id]
  (m/mlet
   [_check-stage (if (> stage-id 0) (monad-db/get-stage-by-id stage-id) (either/right))]
   (monad-db/get-avatars-by-stage-id stage-id)))
(ns cocdan.services.avatar
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [cocdan.db.core :as db]
            [taoensso.nippy :as nippy]
            [cocdan.db.monad-db :as monad-db]))

(defn create-avatar!
  [{:keys [name image description stage substage props]} access-user-id]
  (m/mlet [res (either/try-either
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
    _check-stage (if stage (monad-db/get-stage-by-id stage) (either/right))
    _check-new-controller (if controlled_by (monad-db/get-user-by-id controlled_by) (either/right))]
   (either/try-either
    (db/general-updator
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
      :table "avatars"}))))

(ns cocdan.db.monad-db
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [cocdan.db.core :as db]
            [taoensso.nippy :as nippy]))

(defn get-user-by-username
  [username]
  (let [res (db/get-user-by-username {:username username})]
    (if res 
      (either/right res)
      (either/left (str "用户 " username " 不存在")))))

(defn get-user-by-id
  [user-id]
  (let [res (db/general-get-by-id {:table "users"
                                   :id user-id})]
    (if res
      (either/right res)
      (either/left (str "用户 " user-id " 不存在")))))

(defn get-avatar-by-id
  [avatar-id]
  (m/mlet
   [res (either/try-either
         (db/general-get-by-id {:table "avatars" :id avatar-id}))] 
   (if res
     (either/try-either (-> res (update :props nippy/thaw)))
     (either/left (str "角色 " avatar-id " 不存在")))))

(defn get-avatars-by-stage-id
  [stage-id]
  ())

(defn get-stage-by-id
  ([stage-id recursed]
   (m/mlet
    [raw (either/try-either
          (db/general-get-by-id {:table "stages"
                                 :id stage-id}))]
    (if (nil? raw)
      (either/left (str "舞台 stage-id = " stage-id " 不存在"))
      (either/try-either
       (-> raw
           (update :avatars nippy/thaw)
           (update :substages nippy/thaw))))))
  ([stage-id]
   (m/mlet
    [raw (either/try-either
          (db/general-get-by-id {:table "stages"
                                 :id stage-id}))]
    (if (nil? raw)
      (either/left (str "舞台 stage-id = " stage-id " 不存在"))
      (either/try-either
       (-> raw
           (update :avatars nippy/thaw)
           (update :substages nippy/thaw)))))))




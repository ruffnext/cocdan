(ns cocdan.db.monad-db
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.tools.logging :as log]
            [cocdan.auxiliary :refer [get-db-action-return]]
            [cocdan.db.core :as db]
            [taoensso.nippy :as nippy]
            [cocdan.core.ops.core :as op-core]))

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

(defn- m-extra-avatar-info
  [avatar-db]
  (either/try-either
   (-> avatar-db (update :props nippy/thaw))))

(defn get-avatar-by-id
  [avatar-id]
  (m/mlet
   [res (either/try-either
         (db/general-get-by-id {:table "avatars" :id avatar-id}))] 
   (if res
     (m-extra-avatar-info res)
     (either/left (str "角色 " avatar-id " 不存在")))))

(defn get-avatars-by-user-id
  [user-id]
  (m/mlet
   [res (either/try-either
         (db/get-avatars-by-user-id {:id user-id}))]
   (either/right
    (->> (map m-extra-avatar-info res)
         (either/rights)
         (map m/extract) vec))))

(defn get-avatars-by-stage-id
  [stage-id]
  (m/mlet
   [res (either/try-either
         (db/get-avatars-by-stage-id {:id stage-id}))]
   (either/right
    (->> (map m-extra-avatar-info res)
         (either/rights)
         (map m/extract) vec))))

(defn- m-extra-stage-info
  [stage]
  (either/try-either
   (-> stage
       (update :avatars nippy/thaw)
       (update :substages nippy/thaw))))

(defn get-stage-by-id
  [stage-id]
  (m/mlet
   [raw (either/try-either
         (db/general-get-by-id {:table "stages"
                                :id stage-id}))]
   (if (nil? raw)
     (either/left (str "舞台 stage-id = " stage-id " 不存在"))
     (m-extra-stage-info raw))))

(defn get-stage-latest-ctx-id-by-stage-id
  [stage-id]
  (m/mlet 
   [latest-ctx-id (either/try-either
                   (-> (db/get-stage-latest-context-id {:stage-id stage-id})
                       get-db-action-return))]
   (when latest-ctx-id
     (either/right latest-ctx-id)
     (either/left (str "舞台 " stage-id " 在后端数据库中尚未初始化")))))

(defn- m-extra-stage-context
  [stage-context]
  (either/try-either
   (-> stage-context
       (update :props nippy/thaw))))

(defn get-stage-latest-ctx-by-stage-id
  [stage-id]
  (m/mlet 
   [ctx-id (get-stage-latest-ctx-id-by-stage-id stage-id)]
   (either/try-either
    (-> (db/get-stage-context-by-id {:stage-id stage-id :id ctx-id})
        m-extra-stage-context))))

(defn get-latest-transaction-id-by-stage-id
  [stage-id]
  (m/mlet
   [latest-id (either/try-either
                   (-> (db/get-stage-latest-transaction-id {:stage-id stage-id})
                       get-db-action-return))]
   (when latest-id
     (either/right latest-id)
     (either/left (str "舞台 " stage-id " 在后端数据库中尚未初始化")))))

(defn m-extra-transaction
  [transaction]
  (either/try-either
   (-> transaction
       (update :props nippy/thaw))))

(defn list-stage-transactions-after-n
  "取出的数据中包含 begin-id"
  ([stage-id begin-id limit]
   (m/mlet
    [raw-results (either/try-either
                  (db/list-transactions-after-n {:stage-id stage-id
                                                 :limit limit
                                                 :n begin-id}))]
    (either/right
     (->> raw-results
          (map m-extra-transaction)
          (either/rights)
          (map m/extract) vec))))
  ([stage-id begin-id]
   (list-stage-transactions-after-n stage-id begin-id 100)))


(defn make-stage-snapshot!
  [{stage-id :id :as stage-record}]
  (either/try-either
   (db/insert-transaction! {:id 1
                            :stage stage-id
                            :type op-core/OP-SNAPSHOT
                            :props stage-record})))

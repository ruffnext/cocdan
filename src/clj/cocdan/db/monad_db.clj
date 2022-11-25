(ns cocdan.db.monad-db
  (:require [cats.context :as c]
            [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.tools.logging :as log]
            [cocdan.data.stage :refer [new-context]]
            [cocdan.db.core :as db]
            [taoensso.nippy :as nippy]))

(defn get-db-action-return
  [ret-val]
  (-> ret-val first last))


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
   (-> avatar-db (update :payload nippy/thaw))))

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
         (map #(->> % m/extract (into {}))) vec))))

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

(defn get-stage-latest-ctx_id-by-stage-id
  [stage-id]
  (m/mlet
   [latest-ctx_id (either/try-either
                   (-> (db/get-stage-latest-context-id {:stage-id stage-id})
                       get-db-action-return))]
   (if latest-ctx_id
     (either/right latest-ctx_id)
     (either/left (str "舞台 " stage-id " 在后端数据库中尚未初始化")))))

(defn- m-extra-stage-context
  [stage-context]
  (either/try-either
   (-> stage-context
       (update :payload #(-> % nippy/thaw new-context)))))

(defn get-stage-latest-ctx-by-stage-id
  [stage-id]
  (c/with-context
    either/context
    (m/->=
     (get-stage-latest-ctx_id-by-stage-id stage-id)
     (#(either/try-either
        (db/get-stage-context-by-id {:stage-id stage-id :id %})))
     m-extra-stage-context)))

(defn get-latest-transaction-id-by-stage-id
  [stage-id]
  (m/mlet
   [latest-id (either/try-either
               (-> (db/get-stage-latest-transaction-id {:stage-id stage-id})
                   get-db-action-return))]
   (if latest-id
     (either/right latest-id)
     (either/left (str "舞台 " stage-id " 在后端数据库中尚未初始化")))))

(defn m-extra-transaction
  [transaction]
  (either/try-either
   (-> transaction
       (update :payload nippy/thaw))))

(defn list-stage-transactions
  [stage-id order limit begin offset]
  (let [func (if (= order :desc)
               db/list-transactions-recent-desc
               db/list-transactions-history)]
    (m/mlet
     [begin (if (and (= order :desc) (= 0 begin))
              (either/try-either (-> (db/get-stage-latest-transaction-id {:stage-id stage-id})
                                     first second inc))
              (either/right begin))
      raw-results (either/try-either
                   (func {:stage stage-id :begin begin :limit limit :offset offset}))]
     (let [res (->> raw-results
                    (map m-extra-transaction)
                    (either/rights)
                    (map m/extract))] 
       (if (empty? res)
         (either/left {:status 204 :body []})
         (either/right (vec res)))))))

(defn get-stage-context-by-id
  [stage-id ctx_id]
  (c/with-context
    either/context
    (m/->=
     (either/try-either
      (db/get-stage-context-by-id {:stage-id stage-id :id ctx_id}))
     m-extra-stage-context)))

(defn persistence-transaction!
  [stage-id transaction]
  (let [to-be-insert (-> transaction (assoc :stage stage-id) (update :payload nippy/freeze))] 
    (either/try-either
     (db/insert-transaction! to-be-insert))))

(defn persistence-context!
  [stage-id context]
  (let [to-be-insert (-> context (assoc :stage stage-id) (update :payload nippy/freeze))]
    (either/try-either
     (db/insert-context! to-be-insert))))

(defn flush-stage-to-database!
  "拆分 stage 的信息
    * 将 avatar 中玩家创建的部分持久化到 avatar 表中
    * 将 stage 整体塞入 stage 表中"
  [{:keys [avatars substages] :as stage}] 
  (doseq                                                    ;; 玩家自建角色的 id 从 1 开始
   [{id :id payload :payload :as v} (->> avatars (map second) (filter #(pos-int? (:id %))))]
    (db/general-updater
     {:id id
      :updates (->> (assoc v :payload (nippy/freeze (if payload payload {})))
                    (filter (fn [[_k v]] v))
                    (into {}))
      :table "avatars"}))
  (db/general-updater
   {:id (:id stage)
    :updates (-> stage
                 (assoc :substages (if substages (nippy/freeze substages) nil))
                 (assoc :avatars (if avatars (nippy/freeze avatars) nil))
                 (#(filter (fn [[_k v]] (some? v)) %))
                 (#(into {} %)))
    :table "stages"}))

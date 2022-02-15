(ns cocdan.avatars.auxiliary
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [cocdan.db.core :as db]
            [cocdan.auxiliary :as gaux]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))


(defn list-avatars-by-user?
  ([userId]
   (m/mlet [res (either/try-either (db/list-avatars-by-user? {:controlled_by userId}))]
           (if (empty? res)
             (either/left (format "there is no avatar owned by %d" userId))
             (either/right (map #(gaux/cover-json-field % :attributes) res)))))
  ([userId avatars]
   (let [res (filter #(= (:controlled_by %) userId) avatars)]
     (cond
       (empty? res) (either/left (format "there is no avatar owned by %d" userId))
       :else (either/right res)))))

(defn list-avatars-by-stage?
  ([stageId']
   (m/mlet [stageId (gaux/cover-int-field stageId')
            res (either/try-either (db/list-avatars-by-stage? {:on_stage stageId}))]
           (if (empty? res)
             (either/left (format "there is no avatars on stage %d" stageId))
             (either/right (map #(gaux/cover-json-field % :attributes) res)))))
  ([stageId avatars]
   (let [res (filter #(= (:on_stage %) stageId) avatars)]
     (cond
       (empty? res) (either/left (format "there is no avatars on stage %d" stageId))
       :else (either/right res)))))

(defn list-avatars-by-name?
  [avatarName avatars]
  (let [res (filter #(= (:name %) avatarName) avatars)]
    (cond
      (empty? res) (either/left (format "there is not avatar whose name is %s" avatarName))
      :else (either/right (map #(gaux/cover-json-field % :attributes) res)))))


(defn avatar-check-conflict
  "check for name conflict"
  [avatarName _userId on_stage]
  (let [res (cond
              (nil? on_stage) (either/left "")
              :else (m/->>= (list-avatars-by-stage? on_stage)
                            (list-avatars-by-name? avatarName)))]
    (either/branch res
                   (fn [_] (either/right "there is no conflict"))
                   (fn [v] (either/left (format "conflict by avatar %s" (str (first v))))))))

(defn get-avatar-by-id?
  [avatarId]
  (m/mlet [res (either/try-either (db/get-avatar-by-id? {:id avatarId}))]
          (cond
            (empty? res) (either/left (format "there is no avatar whose id is %d" avatarId))
            :else (either/right (gaux/cover-json-field (first res) :attributes) ))))

(comment
  (log/info
   (get-avatar-by-id? 1))
  )

(defn transfer-avatar!
  [{:keys [name controlled_by on_stage] :as avatar}]
  {:pre [(not (string/blank? name))
         (pos-int? controlled_by)
         (or (nil? on_stage) (pos-int? on_stage))]}
  (m/mlet [avatar'  (if (nil? (:attributes avatar))
                      (either/right avatar)
                      (either/right (assoc avatar :attributes (gaux/->json (:attributes avatar)))))
           _ (either/try-either (db/general-transfer {:table "avatars"
                                                      :updates (dissoc avatar' :id)
                                                      :id (:id avatar)}))]
          (m/return avatar)))

(defn create-avatar!
  [{name :name controlled_by :controlled_by on_stage :on_stage :as avatar}]
  {:pre [(some? name) (string? name)
         (some? controlled_by) (int? controlled_by)]}
  (m/mlet [_ (avatar-check-conflict name controlled_by on_stage)
           res (either/try-either (db/create-avatar! avatar))]
          (either/right res)))

(defn delete-avatar!
  [avatarId]
  (m/mlet [_ (either/try-either (db/delete-avatar-by-id! {:id avatarId}))]
          (either/right (format "delete avatar %d success" avatarId))))
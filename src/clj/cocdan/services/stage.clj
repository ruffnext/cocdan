(ns cocdan.services.stage
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.tools.logging :as log]
            [cocdan.auxiliary :refer [get-last-insert-id]]
            [cocdan.db.core :as db]
            [cocdan.db.monad-db :as monad-db :refer [get-user-by-id]]
            [taoensso.nippy :as nippy]))

(defn create-stage!
  [{:keys [name introduction image substages]} controlled_by]
  (m/mlet [ret (either/try-either
                (db/create-stage!
                 {:name name
                  :introduction introduction
                  :image image
                  :substages (nippy/freeze (or substages []))
                  :avatars (nippy/freeze [])
                  :controlled_by controlled_by}))]
          (monad-db/get-stage-by-id (get-last-insert-id ret))))

(defn query-stage-by-id
  [stage-id]
  (m/mlet
   [stage-base (monad-db/get-stage-by-id stage-id)
    avatars (monad-db/get-avatars-by-stage-id stage-id)]
   (either/right
    (assoc stage-base :avatars avatars))))

(defn check-user-stage-access
  [stage user-id]
  (let [controller-ids (conj (map :controlled_by (:avatars stage)) (:controlled_by stage))]
    (if (contains? (set controller-ids) user-id)
      (either/right)
      (either/left (str user-id " 无权修改舞台 " (:id stage))))))

(defn update-stage!
  [stage-id {:keys [name introduction image substages avatars controlled_by]} access-user-id]
  (m/mlet
   [stage (monad-db/get-stage-by-id stage-id)
    _check-user-access (check-user-stage-access stage access-user-id)
    _check-user-vaildate (if controlled_by
                           (get-user-by-id controlled_by)
                           (either/right))
    _work (either/try-either
           (db/general-updator {:table "stages"
                                :updates (->> {:name name
                                               :introduction introduction
                                               :image image
                                               :substages (if substages (nippy/freeze substages) nil)
                                               :avatars (if avatars (nippy/freeze avatars) nil)
                                               :controlled_by controlled_by}
                                              (filter (fn [[_k v]] (some? v)))
                                              (into {}))
                                :id stage-id}))]
   (query-stage-by-id stage-id)))

(defn delete-stage!
  [stage-id access-user-id]
  (m/mlet
   [{:keys [controlled_by]} (monad-db/get-stage-by-id stage-id)
    _check-user-access (if (= controlled_by access-user-id)
                         (either/right)
                         (either/left (str "用户 user-id=" access-user-id " 没有权限删除 stage-id=" stage-id)))
    avatars (monad-db/get-avatars-by-stage-id stage-id)
    _work (m/for [{id :id} avatars] (either/try-either
                                     (db/general-updator
                                      {:table "avatars"
                                       :updates {:stage 0}
                                       :id id})))]
   (either/branch-left
    (either/try-either
     (db/general-delete {:table "stages"
                         :id stage-id}))
    (fn [_left]   ; 一旦删除失败，简单回滚操作
      (m/for [{id :id} avatars] (either/try-either
                                 (db/general-updator
                                  {:table "avatars"
                                   :updates {:stage stage-id}
                                   :id id})))))))

(defn get-stages-by-user-id
  [user-id]
  (m/mlet [avatars (monad-db/get-avatars-by-user-id user-id)
           stage-ids (->> (map :stage avatars)
                          (filter pos-int?)
                          set either/right)] 
          (->> (map monad-db/get-stage-by-id stage-ids)
               either/rights
               (map m/extract)
               vec either/right)))

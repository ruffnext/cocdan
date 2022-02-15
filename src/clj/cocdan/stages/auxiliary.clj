(ns cocdan.stages.auxiliary
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [cocdan.avatars.auxiliary :refer [transfer-avatar!]]
            [cocdan.db.core :as db]
            [cocdan.auxiliary :as gaux]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

(defn find-by-name?
  "find specified stage"
  ([stageName]
   (m/mlet [res (either/try-either (db/find-stage-by-name? {:name stageName}))]
           (cond
             (empty? res) (either/left (format "there is no stage named %s" stageName))
             :else (either/right (first res)))))
  ([stageName stages]
   (let [res (filter #(= (:name %) stageName) stages)]
     (cond
       (empty? res) (either/left (format "there is no stage named %s" stageName))
       :else (either/right (first res))))))

(defn get-by-id?
  "get a stage by id"
  ([stageId]
  (m/mlet [stageId (cond
                     (pos-int? stageId) (either/right stageId)
                     (nil? stageId) (either/left "please specify stage id")
                     :else (either/try-either (Integer/parseInt stageId)))
           res (either/try-either (db/get-stage-by-id? {:id stageId}))]
          (if (empty? res)
            (either/left (format "there is no stage id = %d" stageId))
            (m/return (gaux/cover-json-field  res :attributes)))))
  ([stageId stages]
   (let [res (filter #(= (:id %) stageId) stages)]
     (cond
       (empty? res) (either/left (format "there is no stage id = %d" stageId))
       :else (either/right res)))))

(comment
  (m/mlet [stage (get-by-id? 1)]
          stage))

(defn get-by-code?
  [code]
  (m/mlet [[sid pass] (let [xs (str/split (str code) #"-")]
                        (if (not= 2 (count xs))
                          (either/left "invalid code")
                          (either/try-either [(Integer/parseInt (first xs)) (second xs)])))
           stage (get-by-id? sid)
           _ (cond
               (= pass (:code stage)) (either/right "")
               :else (either/left "Wrong code"))]
          (m/return (gaux/cover-json-field  stage :attributes))))

(defn create-stage!
  "create a stage"
  [{{code :code :as stage} :stage owned_by :owned_by}]
  (m/mlet [code (cond
                  (or (nil? code)
                      (str/blank? code)) (either/right (gaux/rand-alpha-str 8))
                  :else (either/right code))
           stage (either/try-either (db/create-stage! (assoc stage 
                                                             :owned_by (:id owned_by)
                                                             :code code)))
           _ (transfer-avatar! (assoc owned_by :on_stage (:id stage)))
           stageDetail (get-by-id? (:id stage))]
          (either/right stageDetail)))

(defn check-control-permission
  [avatars stage]
  (if (contains? (set (reduce (fn [a x]
                                (conj a (:id x))) [] avatars)) (:owned_by stage))
    (either/right (first (take 1 (filter #(= (:owned_by stage) (:id %)) avatars))))
    (either/left (format "you have no premission to modify stage %d" (:id stage)))))

(defn delete-stage
  [stage-id]
  (either/try-either (db/delete-stage! {:id stage-id})))

(defn stage-curtain!
  "pause a stage"
  []
  ())

(defn stage-destory!
  "destory a stage"
  []
  ())

(defn avatar-on-stage?
  "check if avatar on this stage"
  []
  ())


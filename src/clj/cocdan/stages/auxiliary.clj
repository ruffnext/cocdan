(ns cocdan.stages.auxiliary
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [cocdan.avatars.auxiliary :refer [transfer-avatar!]]
            [cocdan.db.core :as db]
            [cocdan.auxiliary :as gaux :refer [date-to-timestamp]]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.data.json :as json]))

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

(defn query-an-action?
  [stage-id action-order]
  (m/->= (either/try-either (db/get-action-by-stage-id-and-order {:stage stage-id :order action-order}))
         ((fn [x]
            (if (empty? x)
              (either/left (str "There is no action whose order is " action-order " on stage " stage-id))
              (either/right (-> (gaux/cover-json-field  (first x) :fact)
                                ((fn [x]
                                   (assoc x :time (date-to-timestamp (:time x))))))))))))

(defn query-actions-of-stage?
  [stage-id]
  (m/->= (either/try-either (db/list-actions-by-stage-id? {:stage stage-id}))
         ((fn [xs]
            (if (empty? xs)
              (either/left (str "There is no action on stage " stage-id))
              (either/right (vec (map (fn [x]
                                        (-> (gaux/cover-json-field  x :fact)
                                            ((fn [x]
                                               (assoc x :time (date-to-timestamp (:time x)))))))
                                      xs))))))))

(defn upsert-an-action!
  [{order :order stage-id :stage fact :fact :as action}]
  (either/branch (query-an-action? stage-id order)
                 (fn [_left-val]
                   (either/try-either (db/insert-action! (assoc action 
                                                                :time (new java.util.Date (:time action))
                                                                :fact (json/write-str fact)))))
                 (fn [right-val]
                   (either/try-either (db/update-action!! (assoc action 
                                                                 :time (new java.util.Date (:time right-val))
                                                                 :fact (json/write-str fact)))))))

(defn reset-stage-actions!
  [stage-id]
  (either/try-either (db/clear-history-actions! {:stage stage-id})))

(comment
  (log/debug (upsert-an-action! {:order 1 :time (new java.util.Date) :stage 1 :fact {:hello "World"} :type "msg"}))
  (m/mlet [action (query-an-action? 1 1)]
           action)
  (new java.util.Date)
  (log/debug (query-actions-of-stage? 1))
  (. (new java.util.Date 1645691703808) getTime)
  (db/get-action-by-stage-id-and-order {:stage 1 :order 1})
  (log/debug (reset-stage-actions! 1))
  )
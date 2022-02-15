(ns cocdan.ws.db
  (:require
   [datascript.core :as d]
   [clojure.string :as str]
   [cats.monad.either :as either]
   [cats.core :as m]
   [clojure.tools.logging :as log]
   [cocdan.db.core :refer [general-transfer]]
   [cocdan.auxiliary :as gaux]
   [cocdan.ws.auxiliary :as ws-aux]
   [immutant.web.async :as async]))

; [db/id   :attr              :value                ...]
; [  1     :channel/ws         [Object WebSocket]      ]
; [  1     :channel/stage-id   1                       ]
; [  1     :channel/user       {:name "swz" ...}       ]
; [  1     :                                           ]
; [  2     :stage/id           1                       ]
; [  2     :stage/config       {:foo "bar"}            ]
; [  2     :stage/...                                  ]
; [  3     :avatar/id          1                       ]
; [  3     :avatar/on_stage    1                       ]
;
;
;


(def db-schema
  {:channel/ws {:db/unique :db.unique/identity}
   :stage/id {:db/unique :db.unique/identity}
   :avatar/id {:db/unique :db.unique/identity}})

(defonce db (d/create-conn db-schema))

(defn- handle-key
  [base k]
  (keyword (str (name base) "/" (name k))))

(defn- handle-keys
  [base attrs]
  (reduce (fn [a [k v]]
            (assoc a (handle-key base k) v)) {} attrs))

(defn- remove-perfix
  [k]
  (keyword (first (str/split (name k) #"/" 1))))

(defn remove-db-perfix
  [vals]
  (cond
    (or (vector? vals)
        (list? vals)) (map remove-db-perfix vals)
    :else (reduce (fn [a [k v]]
                    (assoc a (remove-perfix k) v)) {} (dissoc vals :db/id))))

(defn
  query-channels-by-channel
  "query all channels on the same stage with channel"
  [ds channel]
  (->> (d/q '[:find ?channels
              :in $ ?channel
              :where
              [?id :channel/ws ?channel]
              [?id :channel/stage-id ?stage-id]
              [?cids :channel/stage-id ?stage-id]
              [?cids :channel/ws ?channels]]
            ds
            channel)
       (reduce into [])))

(defn
  query-avatars-by-channel
  [ds channel]
  (->> (d/q '[:find ?avatar-eids
              :in $ ?channel
              :where
              [?id :channel/ws ?channel]
              [?id :channel/stage-id ?stage-id]
              [?avatar-eids :avatar/on_stage ?stage-id]]
            ds
            channel)
       (reduce into [])))

(defn
  pull-channel
  [ds channel]
  (->> (d/entid ds [:channel/ws channel])
       (#(cond
           (nil? %) (either/left "No Such Channel")
           :else (either/try-either (-> (d/pull ds '[*] %)
                                        remove-db-perfix))))))

(defn
  pull-stage
  [ds stage-id]
  (->> (d/entid ds [:stage/id stage-id])
       (#(cond
           (nil? %) (either/left "No Such Stage")
           :else (either/try-either (-> (d/pull ds '[*] %)
                                        remove-db-perfix))))))

(defn
  pull-stage-by-channel
  [ds channel]
  (let [stage-eid (->> (d/q '[:find ?eid
                              :in $ ?channel
                              :where
                              [?cid :channel/ws ?channel]
                              [?cid :channel/stage-id ?stage-id]
                              [?eid :stage/id ?stage-id]]
                            ds channel)
                       (reduce into [])
                       first)]
    (either/try-either (-> (d/pull ds '[*] stage-eid)
                           remove-db-perfix))))

(defn
  pull-avatars-by-channel
  [ds channel]
  (let [avatar-eids (query-avatars-by-channel ds channel)]
    (either/try-either 
     (-> (d/pull-many ds '[*] avatar-eids)
         remove-db-perfix))))

(defn
  pull-avatar-by-id
  [ds avatar-id]
  (let [eid (d/entid ds [:avatar/id avatar-id])]
    (if (nil? eid)
      (either/left (format "No Such Avatar id = %d" avatar-id))
      (either/try-either (-> (d/pull ds '[*] eid)
                             remove-db-perfix)))))

(defn upsert!
  "upsert record to in-memory db"
  [ds col-key attrs]
  (let [db-op (cond
                (or (list? attrs)
                    (vector? attrs)
                    (seq? attrs)) (doall (for [attr attrs]
                                              (handle-keys col-key attr)))
                (map? attrs) [(handle-keys col-key attrs)]
                :else [])]
    (d/transact! ds db-op)))

(defn- upsert-persistent-db!
  [col-key attrs]
  (general-transfer {:table (str (name col-key) "s")
                     :updates (-> attrs
                                  (dissoc :id)
                                  (#(if (nil? (:attributes %))
                                      %
                                      (assoc % :attributes (gaux/->json (:attributes %))))))
                     :id (:id attrs)}))

(defn upsert-db!
  "upsert record to in-memory db and sync to persistent database asynchronously"
  [ds col-key attrs]
  (upsert! ds col-key attrs)
  (case col-key
    :stage (upsert-persistent-db! col-key attrs)
    :avatar (upsert-persistent-db! col-key attrs)
    ))

(defn notify-clients-db!
  "send synchronization information to all end users on the same stage"
  ([ds col-key channel]
   (m/mlet [attrs (case col-key
                    :stage (pull-stage-by-channel ds channel)
                    :avatar (pull-avatars-by-channel ds channel)
                    (either/left (str "col-key " col-key " not found")))]
           (notify-clients-db! ds col-key channel attrs)))
  ([_ds col-key channel attrs]
   (let [responseMsg (gaux/->json (ws-aux/make-msg 0 "sync" {:target (name col-key)
                                                             :value attrs}))
         channels (query-channels-by-channel @db channel)]
     (doseq [channel channels]
       (async/send! channel responseMsg)))))

(defn- query-channels-by-stage-id
  [ds stage-id]
  (if (nil? stage-id)
    []
    (->> (d/q '[:find ?channels
                :in $ ?stage-id
                :where
                [?e :channel/stage-id ?stage-id]
                [?e :channel/ws ?channels]]
              ds
              stage-id)
         (reduce into []))))

(defn middleware-ws-update
  "notify ws channels the change of db"
  [{status :status
    {id :id :as val} :body :as response} col-key]
  (when (and (or (= 200 status) (nil? status)) (contains? #{:avatar :stage} col-key))
        (let [channels (case col-key
                         :stage (query-channels-by-stage-id @db id)
                         :avatar (query-channels-by-stage-id @db (:on_stage val))
                         :else [])
              responseMsg (gaux/->json (ws-aux/make-msg 0 "sync" {:target (name col-key)
                                                                  :value val}))]
          (when (seq channels)
            (doseq [channel channels]
              (async/send! channel responseMsg))
            (upsert! db col-key val))))
  response)


(contains? #{:avatar :stage} :stage)

(comment
  (d/entid @db [:stage/id 1])
  (log/info (pull-stage @db 1))
  (let [channel (->> (d/q '[:find ?channel
                            :where
                            [?e :channel/ws ?channel]]
                          @db)
                     (reduce into [])
                     first)]
    ;; (query-channels-by-channel @db channel)
    (notify-clients-db! @db :stage channel))

  
  (d/q '[:find ?eids
         :where [?eids :avatar/id]]
       @db)
  )
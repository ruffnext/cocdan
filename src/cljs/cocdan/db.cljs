(ns cocdan.db
  (:require
   [datascript.core :as d]
   [re-posh.core :as rp]
   [clojure.string :as str]
   [posh.reagent :as p]))

(def schema
  {:stage/id {:db/unique :db.unique/identity}
   :avatar/id {:db/unique :db.unique/identity}
   :avatar/messages {:db/cardinality :db.cardinality/many}
   :message/time {:db/index true}
   :message/receiver {:db/index true}
   })

(defonce db (d/create-conn schema))
(rp/connect! db)

(defn- handle-key
  [base k]
  (keyword (str (name base) "/" (name k))))

(defn handle-keys
  [base attrs]
  (reduce (fn [a [k v]]
            (assoc a (handle-key base k) v)) {} attrs))

(defn- remove-perfix
  [k]
  (keyword (first (str/split (name k) "/" 1))))

(defn remove-db-perfix
  [vals]
  (cond
    (or (vector? vals)
        (list? vals)) (map remove-db-perfix vals)
    :else (reduce (fn [a [k v]]
                    (assoc a (remove-perfix k) v)) {} (dissoc vals :db/id))))



(rp/reg-pull-sub
 :rpsub/entity
 '[*])

(rp/reg-event-ds
 :rpevent/upsert
 (fn [_ds [_ col-key attrs]]
   (cond
     (or (list? attrs)
         (vector? attrs)
         (seq? attrs)) (doall (for [attr attrs]
                                (handle-keys col-key attr)))
     (map? attrs) [(handle-keys col-key attrs)]
     :else [])))

(defn pull-eid
  [ds eid]
  (-> @(p/pull ds '[*] (cond
                         (vector? eid) (first eid)
                         :else eid))
      remove-db-perfix))

(defn pull-eids
  [ds eids]
  (-> @(p/pull-many ds '[*] eids)
      remove-db-perfix))

(def defaultDB
  {:stage-simple {:stage-simple-edit-modal-active false
                  :stage-simple-edit-modal-submit-status "is-primary"
                  :stage-simple-edit-modal {}}
   :login {:username ""
           :email ""
           :username-error nil
           :email-error nil
           :status "default"
           :register-status "default"}
   :avatars []; all avatars
   :stages [] ; all stages
   :user {}   ; you
   :users []  ; all users
   :chat []})


(comment
  (take 10 (d/datoms @db :eavt 2))
  (take 10 (d/q '[:find [?message-time ...]
                  :in $ ?avatar-id
                  :where
                  [?e :message/receiver ?avatar-id]
                  [?e :message/time ?message-time]]
                @db
                2))
  (->> (d/datoms @db :avet  :message/time)
       reverse
       (take 10)
       (map #(vector (:e %) (:a %) (:v %))))
  (->> (d/seek-datoms @db :avet :message/receiver 2)
       reverse
       (take 10)
       (map #(vector (:e %) (:a %) (:v %))))
  (->> (d/datoms @db :avet :message/time)
       reverse
       (map :e)
       (reduce (fn [a x]
                 (let [message (d/pull @db '[*] x)]
                   (if (= (:message/receiver message) 2)
                     (conj a message)
                     a)))
               [])
       (take 10))
  (reduce (fn [a x]
            (let [message (d/pull @db '[*] x)]
              (if (= (:message/receiver message) 2)
                (conj a message)
                a)))
          []
          [8 9 10])

  (d/pull @db '[*] 8)
  (d/q '[:find [?e ...]
         :where
         [?e :message/time 1645431052943]]
       @db)
  (do
    (d/transact! db [{:message/time 1645431052943 :message/receiver 2}]))

  (d/filter @db
            (fn [db datom]
              (or
      ;; leaving all datoms that are not about :person/* as-is
               (not= "avatar" (namespace (:a datom)))
      ;; for :person/* attributes take entity id
      ;; and check :person/name on that entity using db value
               (let [eid    (:e datom)
                     entity (d/entity db eid)]
                 (= "KP" (:avatar/name entity))))))


  (let [dvec #(vector (:e %) (:a %) (:v %))
        db (-> (d/empty-db {:age {:db/index true}})
               (d/db-with [[:db/add 1 :name "Petr"]
                           [:db/add 1 :age 44]
                           [:db/add 2 :name "Ivan"]
                           [:db/add 2 :age 25]
                           [:db/add 3 :name "Sergey"]
                           [:db/add 3 :age 11]]))]
    (map dvec (d/datoms db :avet :age)) ;; name non-indexed, excluded from avet
    )
  )
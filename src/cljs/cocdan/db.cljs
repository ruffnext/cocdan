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
   :log/time {:db/index true}
   :action/order {:db/index true}
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
    (nil? vals) nil
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

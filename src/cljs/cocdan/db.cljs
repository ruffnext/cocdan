(ns cocdan.db
  (:require
   [datascript.core :as d]
   [re-posh.core :as rp]
   [cocdan.auxiliary :refer [handle-keys remove-db-perfix]]
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
  [db eid]
  (when eid
    (-> @(p/pull db '[*] (cond
                           (vector? eid) (first eid)
                           :else eid))
        remove-db-perfix)))

(defn d-pull-eid
  [ds eid]
  (-> (d/pull ds '[*] (cond
                        (vector? eid) (first eid)
                        :else eid))
      remove-db-perfix))

(defn pull-eids
  [ds eids]
  (-> @(p/pull-many ds '[*] eids)
      remove-db-perfix))

(def defaultDB
  {}) ; re-frame db is deprecated

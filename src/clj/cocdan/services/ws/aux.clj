(ns cocdan.services.ws.aux 
  (:require [clojure.string :as str]
            [cocdan.auxiliary :refer [get-current-time-string]]
            [cocdan.core.ops.core :as op-core]
            [cocdan.database.ctx-db.core :refer [check-consistency
                                                 query-ds-latest-ctx-id
                                                 query-ds-latest-transaction-id]]))

(defn- remove-prefix
  [k]
  (keyword (first (str/split (name k) #"/" 1))))

(defn remove-db-prefix
  [db-records]
  (cond
    (nil? db-records) nil
    (or (vector? db-records)
        (list? db-records)) (map remove-db-prefix db-records)
    :else (reduce (fn [a [k v]]
                    (assoc a (remove-prefix k) v)) {} (dissoc db-records :db/id))))

(defn- add-db-prefix-aux
  [base k]
  (keyword (str (name base) "/" (name k))))

(defn add-db-prefix
  [base attrs]
  (reduce (fn [a [k v]]
            (if v
              (assoc a (add-db-prefix-aux base k) v)
              a)) {} attrs))

(defn snapshot-if-not-consistency!
  "如果当前状态与给定状态不一致，则生成一个新的 snapshot"
  [db stage-record]
  (let [ds @db]
    (when-not (check-consistency @db stage-record)
      (let [latest-ctx-id (or (query-ds-latest-ctx-id ds) 0)
            latest-transaction-id (or (query-ds-latest-transaction-id ds) 0)
            snapshot (op-core/make-op
                      (inc latest-transaction-id)
                      latest-ctx-id
                      (get-current-time-string)
                      :snapshot stage-record)]
        ;todo transact!
        ))))
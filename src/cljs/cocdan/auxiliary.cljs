(ns cocdan.auxiliary
  (:require
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defn init-page
  [subscriptions event-handler]
  (doseq [[sub f] subscriptions]
    (rf/reg-sub sub #(f %)))
  (doseq [[event f] event-handler]
    (rf/reg-event-fx event (fn [app-data [driven-by & event-args]]
                             (apply f app-data driven-by event-args)))))

(defn <-json
  "from json str to map"
  [val]
  (js->clj (.parse js/JSON val) :keywordize-keys true))

(defn ->json
  "from map to json str"
  [kv]
  (.stringify js/JSON (clj->js kv)))

(defn- handle-key
  [base k]
  (keyword (str (name base) "/" (name k))))

(defn handle-keys
  [base attrs]
  (reduce (fn [a [k v]]
            (if v
              (assoc a (handle-key base k) v)
              a)) {} attrs))

(defn- remove-perfix
  [k]
  (keyword (first (str/split (name k) #"/" 1))))

(defn remove-db-perfix
  [vals]
  (cond
    (nil? vals) nil
    (or (vector? vals)
        (list? vals)) (map remove-db-perfix vals)
    :else (reduce (fn [a [k v]]
                    (assoc a (remove-perfix k) v)) {} (dissoc vals :db/id))))

(defn rebuild-action-from-tx-data
  [tx-data]
  (let [eids (reduce (fn [a [eid attr & _r]]
                       (if (= attr :action/order) (conj a eid) a))
                     [] tx-data)
        res (reduce (fn [a [eid attr val & _r]]
                      (if (contains? (set eids) eid)
                        (assoc-in a [(.indexOf eids eid) attr] val)
                        a))
                    (vec (map (fn [x] {:eid x}) eids))
                    tx-data)]
    (map remove-db-perfix res)))

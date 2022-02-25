(ns cocdan.auxiliary
  (:require
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defn swap-filter-list-map!
  [xs f transform]
  (let [res (map-indexed (fn [i x]
                           (if (f x)
                             i
                             nil)) xs)
        fres (filter #(not (nil? %)) res)]
    (if (empty? fres)
      (conj xs (transform nil))
      (reduce (fn [a x]
                (assoc-in a [x] (transform (nth xs x)))) xs fres))))

(defn init-page
  [subscriptions event-handler]
  (doseq [[sub f] subscriptions]
    (rf/reg-sub sub #(f %)))
  (doseq [[event f] event-handler]
    (rf/reg-event-fx event (fn [app-data [driven-by & event-args]]
                             (apply f app-data driven-by event-args)))))

(defn- handle-list-map-conj
  [db _ keys val _]
  (cond
    (or (list? val)
        (vector? val)) (reduce (fn [a x]
                                 (handle-list-map-conj a "" keys x "")) db val)
    :else (let [target-list' (reduce (fn [a f] (f a)) db keys)
                target-list (if (nil? target-list') [] target-list')]
            (assoc-in db keys (swap-filter-list-map! target-list #(= (:id %) (:id val)) (fn [x] (merge x val)))))))

(rf/reg-event-db
 :event/general-list-map-conj
 (fn [db [driven-by & event-args]]
   (apply handle-list-map-conj db driven-by event-args)))

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
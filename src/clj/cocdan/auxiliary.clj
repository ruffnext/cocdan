(ns cocdan.auxiliary
  (:require
   [jsonista.core :as json]
   [cats.monad.either :as either]
   [clojure.tools.logging :as log]
   [clojure.string :as str]))

(defn rand-alpha-str
  [len]
  (apply str (map char (map (fn [_] (let [r (rand-int 52)]
                                      (if (< r 26)
                                        (+ r 65)
                                        (+ r (- 97 26))))) (take len (range))))))

(def mapper (json/object-mapper {:decode-key-fn keyword}))
(defn <-json
  "json to map"
  [val]
  (json/read-value val mapper))
(defn ->json
  "map to json"
  [val]
  (json/write-value-as-string val))

(defn cover-json-field
  [data field-key]
  (if (map? (field-key data))
    data
    (let [res (assoc data field-key (<-json (field-key data)))]
      (if (map? (field-key res))
        res
        (cover-json-field res field-key)))))

(defn timestamp-to-date
  [timestamp]
  (new java.util.Date timestamp))

(defn date-to-timestamp
  [date]
  (. (. java.sql.Timestamp valueOf date) getTime))

(defn cover-int-field
  [val]
  (cond
    (string? val) (either/try-either (Integer/parseInt val))
    (int? val) (either/right val)
    :else (either/try-either (Integer/parseInt val))))

(defn- get-key
  [prefix key]
  (if (nil? prefix)
    key
    (str prefix "/" key)))
(defn- flatten-map-kvs
  ([map] (flatten-map-kvs map nil))
  ([map prefix]
   (reduce
    (fn [memo [k v]]
      (if (map? v)
        (concat memo (flatten-map-kvs v (get-key prefix (name k))))
        (conj memo [(get-key prefix (name k)) v])))
    [] map)))

(defn flatten-map
  [m]
  (into {} (for [[k v] (flatten-map-kvs m)]
             {(keyword k) v})))

(flatten-map {:owned_by 12 :fromUser {:id 12 :url "something"}})

(defn reconstruct-map-kvs
  ([m]
   (reconstruct-map-kvs m "/"))
  ([m p]
   (reduce (fn [a [k v]]
             (if (nil? v)
               a
               (assoc-in a (map keyword (str/split (subs (str k) 1) (re-pattern p))) v))) {} m)))

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



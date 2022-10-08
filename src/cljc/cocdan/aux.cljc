(ns cocdan.aux
  (:require [clojure.string :as s]
            #?(:clj [jsonista.core :as json])))

;; 全局辅助函数

(def separator ".")
(def separator-reg #"\.")

(defn flatten-nested-map
  [d]
  (if (map? d)
    (reduce-kv
     (fn [a k v]
       (if (map? v)
         (->> (flatten-nested-map v)
              (reduce-kv
               (fn [aa kk vv]
                 (assoc aa (keyword (str (name k) separator (name kk))) vv))
               {})
              (merge a))
         (assoc a k v))) {} d)
    d))

(defn construct-flatten-map
  [d]
  (if (map? d)
    (reduce-kv
     (fn [a k v]
       (assoc-in a (map #(keyword (s/replace % ":" "")) (s/split (name k) separator-reg)) v))
     {} d)
    d))


(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as _keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn- remove-prefix
  [k]
  (keyword (first (s/split (name k) #"/" 1))))

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

(defn datetime-to-string
  [dt]
  #?(:cljs (.toJSON dt))
  #?(:clj (.toString dt)))

(defn get-current-time-string
  []
  #?(:cljs (.toJSON (js/Date.)))
  #?(:clj (.toString (java.time.Instant/now))))

(defn datetime-string-to-datetime
  [time-string]
  #?(:cljs (js/Date. time-string)))

#?(:clj (def mapper (json/object-mapper {:decode-key-fn keyword})))

(defn <-json
  "json to map"
  [val]
  #?(:clj (json/read-value val mapper)
     :cljs (js->clj (.parse js/JSON val) :keywordize-keys true)))

(defn >-json
  "map to json"
  [val]
  #?(:clj (json/write-value-as-string val)
     :cljs (.stringify js/JSON (clj->js val))))

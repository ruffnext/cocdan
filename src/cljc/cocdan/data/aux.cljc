(ns cocdan.data.aux 
  (:require [clojure.string :as s]))

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
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))
(ns cocdan.data.core
  (:require [clojure.set :as cs]
            [cocdan.aux :as data-aux]
            [clojure.string :as s]
            [malli.core :as mc]))

"diffs  --> [paths before after]
 paths  --> a.b.c.d.e
 before --> value or :unset
 after  --> value or :unset"

(defn- calc-value-diff
  [k before after]
  (cond
    (= before after) []

    (= (type before) (type after))
    (cond
      (set? before) (let [added (cs/difference after before)
                          removed (cs/difference before after)]
                      (-> (concat (map (fn [x] [k :remove x]) added)
                                  (map (fn [x] [k x :remove]) removed))
                          vec))
      :else [[k before after]])

    :else [[k before after]]))

(defn diff'
  [before after]
  (let [flatten-before (data-aux/flatten-nested-map before)
        flatten-after (data-aux/flatten-nested-map after)
        aux (fn [k]
              (let [before (or (k flatten-before) :unset)
                    after (or (k flatten-after) :unset)]
                (calc-value-diff k before after)))]
    (vec (apply concat (map aux (set (concat (keys flatten-before) (keys flatten-after))))))))

(defn- handle-update
  [before current after]
  (case before
    :unset after
    :remove (conj current after)
    (case after
      :remove (disj current before)
      after)))

(mc/=> update' [:=> [:cat associative? [:vector [:cat :keyword :any :any]]] associative?])
(defn update'
  [before diffs] 
  (->> (reduce (fn [a [k before after]]
                 (let [ks (map keyword (s/split (name k) #"\."))] 
                   (case after
                     :unset (data-aux/dissoc-in a ks)
                     (update-in a ks #(handle-update before % after))))) before diffs)
       (data-aux/construct-flatten-map)
       (reduce (fn [a [k v]] (assoc a k v)) before)))


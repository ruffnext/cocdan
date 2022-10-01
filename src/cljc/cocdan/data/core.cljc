(ns cocdan.data.core
  (:require [clojure.set :as cs]
            [cocdan.data.aux :as data-aux]
            [clojure.string :as s]))

;; 由 Transaction 得到的，或者具有 Transaction ID 的类型
(defprotocol ITransaction
  (get-tid [this] "取得在事件的编号，一般用于判断先后顺序")
  (get-time [this] "取得事件发生的时间") 
  (get-ctx_id [this] "取得事件上下文的编号，通常用于取得其上下文"))

"diffs  --> [paths before after]
 paths  --> a.b.c.d.e
 before --> value or :unset
 after  --> value or :unset"

(defprotocol IIncrementalUpdate
  "可以增量更新的类型" 
  
  (diff'
    [before after]
    "计量两者的差，并返回 diffs 列表")
  (update'
    [before diffs]
    "按顺序将 diffs 应用到当前对象上，并返回修改后的对象"))

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

(defn default-diff'
  [before after]
  (let [flatten-before (data-aux/flatten-nested-map before)
        flatten-after (data-aux/flatten-nested-map after)
        aux (fn [k]
              (let [before (or (k flatten-before) :unset)
                    after (or (k flatten-after) :unset)]
                (calc-value-diff k before after)))]
    (apply concat (map aux (set (concat (keys flatten-before) (keys flatten-after)))))))

(defn- handle-update
  [before current after]
  (case before
    :unset after
    :remove (conj current after)
    (case after
      :remove (disj current before)
      after)))

(defn default-update'
  [before diffs] 
  (->> (reduce (fn [a [k before after]]
                 (let [ks (map keyword (s/split (name k) #"\."))]
                   (case after
                     :unset (data-aux/dissoc-in a ks)
                     (update-in a ks #(handle-update before % after))))) before diffs)
       (data-aux/construct-flatten-map)
       (reduce (fn [a [k v]] (assoc a k v)) before)))

(ns cocdan.data.transaction.dice 
  (:require [cocdan.data.core :refer [default-update']]
            [cocdan.data.performer.core :refer [get-attr]]))

(defrecord RC [avatar attr attr-val dice-result])

(defrecord ST [avatar attr-map])

(defn handle-st-context
  [{ctx :context/props} {{:keys [avatar attr-map]} :props}] 
  (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])
        ops (reduce (fn [a [k v]]
                      (let [avatar-attr-val (get-attr avatar-record (name k))]
                        (if (= avatar-attr-val v)
                          a (conj a [(keyword (str "avatars." avatar ".props.attrs." (name k))) avatar-attr-val v]))))
                    [] attr-map)]
    (default-update' ctx ops)))

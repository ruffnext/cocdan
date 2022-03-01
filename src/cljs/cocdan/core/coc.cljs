(ns cocdan.core.coc
  (:require
   [cljs.core :as cc]
   [cats.monad.either :as either]
   [cats.core :as m]))

(defn- validate-general-attributes
  [{{{val :attrs} :coc} :attributes}]
  (either/right val))

(defn- get-attr
  [attrs col-key]
  (reduce (fn [a f] (f a)) attrs (conj [:attributes :coc :attrs] col-key)))

(defn- set-attr
  [attrs col-key value]
  (assoc-in attrs (conj [:attributes :coc :attrs] col-key) value))


(defn- handle-formula
  [attrs f]
  (cond
    (list? f) (let [res (reduce (fn [a x]
                                  (let [res (handle-formula attrs x)]
                                    (when (and a res)
                                      (conj a res)))) [] f)]
                (when (seq res)
                  (apply (first res) (rest res))))
    (keyword? f) (get-attr attrs f)
    :else f))

(defn- attr-formula
  [attrs col-key formula]
  (set-attr attrs col-key (handle-formula attrs formula)))


(defn complete-coc-avatar-attributes
  [_avatar-before avatar-now]
  (-> avatar-now
      (attr-formula :max-hp (list quot (list + :con :siz) 10))
      (attr-formula :mov 8)))

(defn validate-coc-avatar
  [avatar]
  (m/->=
   (validate-general-attributes avatar)))
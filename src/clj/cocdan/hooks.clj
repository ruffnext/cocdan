(ns cocdan.hooks 
  (:require [cats.core :as m] 
            [cats.context :as c]
            [clojure.tools.logging :as log]
            [cats.monad.either :as either]))

(defonce hooks (atom {}))

(defn hook!
  [event-key func]
  (let [func-set (or (event-key @hooks) #{})]
    (when-not (contains? func-set func)
      (swap! hooks #(assoc % event-key func-set)))))

(defn dispatch!
  [event-key & args]
  (let [func (event-key @hooks)]
    (when-not (empty? func)
      (c/with-context
       either/context
       (m/for [f func]
        (apply f args))))))


(ns cocdan.hooks 
  (:require [cats.core :as m] 
            [cats.context :as c]
            [clojure.tools.logging :as log]
            [cats.monad.either :as either]))

(defonce hooks (atom {}))

(defn hook!
  "所有的 hook 函数都应该返回一个 either"
  [event-key hook-key func]
  (swap! hooks #(assoc-in % [event-key hook-key] func)))

(defn dispatch!
  [event-key & args]
  (let [funcs (map (fn [[_k v]] v) (event-key @hooks))]
    (if (empty? funcs)
      (either/right (str event-key " 没有注册钩子函数"))
      (c/with-context
        either/context
        (m/for [f funcs]
          (apply f args))))))

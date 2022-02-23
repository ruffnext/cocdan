(ns cocdan.shell.router
  (:require
   [cats.monad.either :as either]
   [cats.core :as m]
   [cocdan.shell.substage :refer [substage]]
   [cocdan.shell.coc.core :refer [coc]]
   [clojure.tools.logging :as log]))

#_{:clj-kondo/ignore [:unused-private-var]}
(defn- module-handler
  [module {msg :msg :as request}]
  (if (not (nil? module)) (let [metas (meta module)
                                matcher (:matcher metas)
                                match-res (re-matcher matcher msg)
                                arglists (map (fn [x] (subs x 2 (- (count x) 1))) (re-seq #"\?<[A-z\-\!\?\'\+]+>" (str matcher)))]
                            (cond
                              (and
                               (not (nil? match-res))
                               (.matches match-res)) (let [args (reduce (fn [a x]
                                                                          (assoc a (keyword x) (.group match-res (str x)))) {}  arglists)]
                                                       (module args request))
                              :else (either/right "")))
      (either/right "")))

(defn handle-msg
  [msg channel]
  (m/->= (either/right msg)
   (substage channel)
   (coc channel)))
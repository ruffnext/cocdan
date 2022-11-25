(ns cocdan.middleware.monad-api
  (:require [cats.monad.either :as either]
            [clojure.tools.logging :as log]))

(defn- handle-api-response
  [res default-status]
  (cond

    (nil? res) {:status default-status :body {:status "ok"}}

    (or (instance? clojure.lang.PersistentArrayMap res)
        (instance? clojure.lang.PersistentHashMap res))
    (if
     (and (contains? res :status) (contains? res :body)) res
     {:status default-status :body res})

    (instance? clojure.lang.PersistentVector res)
    {:status default-status :body res}

    (string? res)
    {:status default-status :body res}

    :else
    {:status default-status :body (str res)}))

(defn wrap-monad
  [handler] 
  (fn [args]
    (either/branch
     (handler args)
     (fn [{:keys [status body] :as res}]
       (cond
         (instance? clojure.lang.PersistentArrayMap res)
         (cond
           (and (pos-int? status) (> status 200) (< status 400)) (handle-api-response body status)
           (and (some? status) (contains? body :error)) (handle-api-response body res)
           :else (handle-api-response {:error res} 400))
         :else (handle-api-response {:error res} 400)))
     #(handle-api-response % 200))))
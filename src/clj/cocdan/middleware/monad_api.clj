(ns cocdan.middleware.monad-api
  (:require [cats.monad.either :as either]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as response]))

(defn wrap-monad
  [handler]
  (fn [request]
    (either/branch
     (handler request)
     (fn [left]
       (response/bad-request {:error (str left)}))
     (fn [right] 
       (cond
         (map? right) 
         (if (contains? right :body)
           (if (contains? right :status)
             right
             (assoc right :status 200))
           {:body right :status 200})
         
         (vector? right) {:status 200 :body right}

         :else {:body {:result right} :status 200})))))
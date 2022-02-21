(ns cocdan.ws.services 
  (:require 
   [cocdan.ws.core :as ws]
   [clojure.tools.logging :as log]
   [cats.monad.either :as either]))

(defn service-routes []
  ["/ws/:stage" #(-> %
                     ws/ws-handler
                     (either/branch
                      (fn [x] {:status 401 :body (str x)})
                      (fn [x] x)))])
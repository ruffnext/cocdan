(ns cocdan.ws.services 
  (:require 
   [cocdan.ws.core :as ws]))

(defn service-routes []
  ["/ws/:stage" ws/ws-handler])
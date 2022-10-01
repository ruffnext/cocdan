(ns cocdan.services.ws.route 
  (:require [cocdan.middleware :refer [wrap-restricted]] 
            [cocdan.services.ws.core :refer [websocket-callbacks]]
            [immutant.web.async :as async]))

(defn ws-routes []
  ["/ws/:stage" (wrap-restricted
                 (fn [request]
                   (async/as-channel request websocket-callbacks)))])
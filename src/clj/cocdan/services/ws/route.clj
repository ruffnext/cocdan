(ns cocdan.services.ws.route 
  (:require [cocdan.middleware :refer [wrap-restricted]] 
            [cocdan.services.ws.core :refer :all]
            [immutant.web.async :as async]))

(defn ws-routes []
  ["/ws/:stage" (fn [request]
                  (async/as-channel request websocket-callbacks))])
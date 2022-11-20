(ns cocdan.env
  (:require [clojure.tools.logging :as log]
            [cocdan.dev-middleware :refer [wrap-dev]]
            [malli.dev :as md]
            [malli.dev.pretty :as pretty]
            [selmer.parser :as parser]))

(def defaults
  {:init
   (fn []
     (md/start! {:report (pretty/reporter)})
     (parser/cache-off!)
     (log/info "\n-=[cocdan started successfully using the development profile]=-"))
   :stop
   (fn []
     (md/stop!)
     (log/info "\n-=[cocdan has shut down successfully]=-"))
   :middleware wrap-dev})

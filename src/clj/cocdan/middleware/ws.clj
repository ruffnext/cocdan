(ns cocdan.middleware.ws
  (:require
   [cocdan.ws.db :as ws-db]
   [clojure.tools.logging :as log]
   [cocdan.shell.db :refer [make-snapshot!]]))

(defn middleware-ws-update
  "notify ws channels the change of db"
  [ {status :status
    {id :id :as body} :body :as response} col-key]
  (when (and (or (= 200 status) (= 201 status) (nil? status)) (contains? #{:avatar :stage} col-key))
    (when-let [stage-id (case col-key
                          :stage id
                          :avatar (:on_stage body)
                          :else nil)]
      (make-snapshot! stage-id)))
  response)

(comment
  (log/debug 
   (ws-db/pull-avatar-by-id @ws-db/db 2))
  )
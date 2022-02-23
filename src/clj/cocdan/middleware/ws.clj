(ns cocdan.middleware.ws
  (:require
   [cats.core :as m]
   [cocdan.ws.db :as ws-db]
   [cats.monad.either :as either]
   [cocdan.shell.db :as s-db]
   [clojure.tools.logging :as log]))

(defn middleware-ws-update
  "notify ws channels the change of db"
  [{status :status
    {id :id} :body :as response} col-key]
  (when (and (or (= 200 status) (nil? status)) (contains? #{:avatar :stage} col-key))
    (m/mlet [stage-id (cond
                        (= col-key :avatar) (m/->=
                                             (ws-db/pull-avatar-by-id @ws-db/db id)
                                             ((fn [x]
                                                (either/right (:on_stage x)))))
                        (= col-key :stage) (either/right id)
                        :else (either/left ""))]
            (log/debug stage-id)
            (s-db/make-snapshot! stage-id)))
  response)

(comment
  (log/debug 
   (ws-db/pull-avatar-by-id @ws-db/db 2))
  )
(ns cocdan.middleware.ws
  (:require
   [cats.core :as m]
   [cocdan.shell.db :refer [action!]]
   [cocdan.ws.db :as ws-db]
   [cocdan.avatars.auxiliary :as avatars-aux]
   [cocdan.stages.auxiliary :as stages-aux]
   [cats.monad.either :as either]))

(defn middleware-ws-update
  "notify ws channels the change of db"
  [{status :status
    {id :id} :body :as response} col-key]
  (when (and (or (= 200 status) (nil? status)) (contains? #{:avatar :stage} col-key))
    (m/mlet [stage-id (cond
                        (= col-key :avatar) (ws-db/pull-avatar-by-id @ws-db/db id)
                        (= col-key :stage) (either/right id)
                        :else (either/left ""))
             avatars (avatars-aux/list-avatars-by-stage? stage-id)
             stage (stages-aux/get-by-id? stage-id)]
     (action! stage-id "snapshot" {:avatars avatars
                                   :stage stage})))
  response)
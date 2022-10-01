(ns cocdan.services.auth.core
  (:require [cats.core :as m]
            [cats.monad.either :as either] 
            [cocdan.db.core :as db]
            [cocdan.db.monad-db :as monad-db]))

(defn register!
  [username nickname]
  (m/mlet [res (either/try-either
                (db/create-user! {:username username :nickname nickname}))]
          (monad-db/get-user-by-id (monad-db/get-db-action-return res))))

(defn unregister!
  [username]
  (m/mlet [{:keys [id]} (monad-db/get-user-by-username username)
           _ (either/try-either
              (db/general-delete {:table "users"
                                  :id id}))]
          (either/right {:status 204 :body {}})))
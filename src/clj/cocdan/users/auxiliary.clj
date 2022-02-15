(ns cocdan.users.auxiliary
  (:require [cocdan.db.core :as db]
            [cocdan.auxiliary :as gaux]
            [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn get-user-by-email?
  [{email :email :as parameters}]
  (m/mlet [res (either/try-either (db/get-user-by-email? parameters))]
          (if (empty? res)
            (either/left {:error (format "user %s does not exists" email)})
            (either/right (gaux/cover-json-field (first res) :config)))))

(defn get-by-id?
  [userId]
  {:per [(pos-int? userId)]}
  (m/mlet [res (either/try-either (db/get-user-by-id? {:id userId}))]
          (if (empty? res)
            (either/left (format "user id = %d does not exists" userId))
            (either/right (first res)))))

(defn register!
  [{email :email name :name :as user}]
  {:pre [(not (string/blank? email))
         (not (string/blank? name))]}
  (either/try-either (db/register-user! user)))

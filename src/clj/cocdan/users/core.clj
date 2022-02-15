(ns cocdan.users.core
  (:require
   [cats.monad.either :as either]))

(defn login?
  [{{userId :id :as user} :user}]
  (if (nil? userId)
    (either/left {:error "you should login first"
                  :status 401})
    (either/right user)))


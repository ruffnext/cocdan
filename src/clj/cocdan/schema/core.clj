(ns cocdan.schema.core
  (:require
   [cats.monad.either :as either]))

(def SchemaUser
  {:id string?
   :name string?
   :email string?})

(def SchemaError
  {:error string?})

(def SchemaAvatar
  {:id int?
   :name string?
   :header string?
   :controlled_by int?
   :on_stage int?
   :attributes map?})

(def SchemaStage
  {:id int?
   :title string?
   :owned_by int?
   :status string?
   :introduction string?
   :banner string?
   :attribute string?})

(defn middleware-either-api
  [res]
  (either/branch res
                 (fn [parameters]
                   (cond
                     (instance? clojure.lang.PersistentArrayMap parameters) ((fn [{:keys [status error] :or {status 400 error ""}}]
                                                                               {:status status :body {:error error}}) parameters)
                     (instance? String parameters) {:status 400 :body {:error parameters}}
                     :else (middleware-either-api (either/left (str parameters)))))
                 (fn [val] val)))

(defmacro invert-left
  [e s]
  `(either/branch-left (either/invert ~e) ~s))
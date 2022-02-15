(ns cocdan.request.avatar 
  (:require
   [cljs-http.client :as http]
   [cats.monad.either :as either]
   [clojure.core.async :refer [<!]]))

(defn create-avatar!
  [avatar]
  (let [res (<! (http/post "api/avatar/create" {:json-params avatar}))]
    (if (= 201 (:status res))
      (either/right res)
      (either/left res))))


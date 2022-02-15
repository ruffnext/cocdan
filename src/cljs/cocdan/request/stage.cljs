(ns cocdan.request.stage
  (:require
   [cljs-http.client :as http]
   [cats.monad.either :as either]
   [clojure.core.async :refer [<! go]]))

(defn join-stage
  [avatarId code]
  (go
    (let [res (<! (http/post "api/stage/join-by-code" {:json-params {:avatar avatarId}
                                                        :query-params {:code code}}))]
      (if (= 200 (:status res))
        (either/right res)
        (either/left res)))))
(ns cocdan.files.core
  (:require [cats.monad.either :as either]
            [clojure.tools.logging :as log]
            [cocdan.config :refer [env]]
            [cocdan.auxiliary :as gaux]
            [clojure.java.io :as io]))


(defn upload!
  [file suffix _]
  (let [file-name (format "%s%s.%s" (:upload-file-path env) (gaux/rand-alpha-str 32) suffix)
        file-path (format "%s/resources/public/%s" (System/getProperty "user.dir") file-name)
        res (either/try-either (io/copy (io/file (.getAbsolutePath file)) (io/file file-path)))]
    (either/branch res
                   #(either/left %)
                   (fn [_] (either/right {:status 200
                                          :body {:name file-name}})))))

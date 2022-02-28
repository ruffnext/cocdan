(ns cocdan.files.core
  (:require
   [cats.monad.either :as either]
   [cats.core :as m]
   [clojure.tools.logging :as log]
   [clojure.data.csv :as csv]
   [cocdan.config :refer [env]]
   [cocdan.auxiliary :as gaux :refer [csv-data->maps]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.data.json :as json]))


(defn upload-img!
  [file suffix _]
  (let [file-name (format "%s.%s" (gaux/rand-alpha-str 32) suffix)
        file-path (format "%s/resources/public/%s%s" (System/getProperty "user.dir") (:upload-file-path env) file-name)
        res (either/try-either (io/copy (io/file (.getAbsolutePath file)) (io/file file-path)))]
    (either/branch res
                   #(either/left %)
                   (fn [_] (either/right {:status 200
                                          :body {:name (str "api/files/image/" file-name)}})))))

(defn- handle-csv
  [file-path]
  (m/mlet [file (let [res (io/resource file-path)]
                  (if res (either/right res) (either/left "file not found")))]
          (m/return {:content-type "application/json"
                     :body (-> (io/reader file)
                               csv/read-csv
                               csv-data->maps
                               json/write-str)})))

(defn fetch-resource!
  [file-path]
  (let [suffix (last (str/split file-path #"\."))]
    (case (str/lower-case suffix)
      "csv" (handle-csv file-path)
      (either/left (str "can't handle file type " suffix)))))

(defn path_validate?
  [file-path]
  (either/right file-path))

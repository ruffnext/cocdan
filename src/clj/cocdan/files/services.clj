(ns cocdan.files.services 
  (:require [clojure.java.io :as io]
            [reitit.ring.middleware.multipart :as multipart]
            [cocdan.schema.core :as schema]
            [cocdan.users.core :as users]
            [cocdan.files.core :as core]
            [cocdan.config :refer [env]]
            [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

(defn upload!
  [{{{{_size :size content-type :content-type file :tempfile} :file} :multipart} :parameters session :session}]
  (m/mlet [_ (users/login? session)
           file-suffix (case content-type
                         "image/png" (either/right "png")
                         "image/jpg" (either/right "jpg")
                         "image/gif" (either/right "gif")
                         "image/jpeg" (either/right "jpeg")
                         (either/left (str "content type " content-type " is not recognized")))
           res (core/upload! file file-suffix (select-keys env [:upload-file-path]))]
          (either/right res)))

(let [file-name "hello.gif"]
  (str/split file-name #"\."))

(def service-routes
  ["/files"
   {:swagger {:tags ["files"]}}

   ["/upload"
    {:post {:summary "upload a file"
            :parameters {:multipart {:file multipart/temp-file-part}}
            :responses {200 {:body {:name string?}}}
            :handler #(-> %
                          upload!
                          schema/middleware-either-api)}}]

   ["/download"
    {:get {:summary "downloads a file"
           :swagger {:produces ["image/png"]}
           :handler (fn [_]
                      {:status 200
                       :headers {"Content-Type" "image/png"}
                       :body (-> "public/img/warning_clojure.png"
                                 (io/resource)
                                 (io/input-stream))})}}]])
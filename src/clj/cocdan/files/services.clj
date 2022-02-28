(ns cocdan.files.services 
  (:require [clojure.java.io :as io]
            [reitit.ring.middleware.multipart :as multipart]
            [cocdan.schema.core :as schema]
            [cocdan.users.core :as users]
            [cocdan.files.core :as core]
            [cocdan.config :refer [env]]
            [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.string :as str]))

(defn upload-img!
  [{{{{_size :size content-type :content-type file :tempfile} :file} :multipart} :parameters session :session}]
  (m/mlet [_ (users/login? session)
           file-suffix (case content-type
                         "image/png" (either/right "png")
                         "image/jpg" (either/right "jpg")
                         "image/gif" (either/right "gif")
                         "image/jpeg" (either/right "jpeg")
                         (either/left (str "content type " content-type " is not recognized")))
           res (core/upload-img! file file-suffix (select-keys env [:upload-file-path]))]
          (either/right res)))

(defn download-image!
  [{{{filename :filename} :path} :parameters session :session}]
  (m/mlet [_ (users/login? session)
           content-type (case (last (str/split filename #"\."))
                          "png" (either/right "image/png")
                          "jpg"  (either/right "image/jpg")
                          "gif" (either/right "image/gif")
                          "jpeg" (either/right "image/jpeg")
                          (either/left (str  filename " is not supported")))]
          (let [file-name (str (:upload-file-path env) filename)
                file-path (format "%s/resources/public/%s" (System/getProperty "user.dir") file-name)]
            (either/try-either {:status 200
                                :headers {"Content-Type" content-type}
                                :body (-> file-path
                                          (io/input-stream))}))))

(defn fetch-resource!
  [{{{file-path :file-path} :path} :parameters session :session}]
  (m/mlet [_ (users/login? session)
           _ (m/do-let
              (core/path_validate? file-path))
           {content-type :content-type body :body} (core/fetch-resource! file-path)]
          (m/return {:status 200
                     :headers {"Content-Type" content-type}
                     :body body})))

(def service-routes
  ["/files"
   {:swagger {:tags ["files"]}}
   ["/upload/image"
    {:post {:summary "upload an image"
            :parameters {:multipart {:file multipart/temp-file-part}}
            :responses {200 {:body {:name string?}}}
            :handler #(-> %
                          upload-img!
                          schema/middleware-either-api)}}]
   ["/image/:filename"
    {:get {:summary "fetch an image"
           :swagger {:produces ["image/png"]}
           :parameters {:path {:filename string?}}
           :handler #(-> %
                         download-image!
                         schema/middleware-either-api)}}]
   ["/res/:file-path"
    {:get {:summary "fetch a resource"
           :parameters {:path {:file-path string?}}
           :handler #(-> %
                         fetch-resource!
                         schema/middleware-either-api)}}]])
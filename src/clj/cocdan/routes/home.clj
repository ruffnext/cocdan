(ns cocdan.routes.home
  (:require
   [clojure.core]
   [cocdan.layout :as layout]
   [cocdan.db.core :as db]
   [cocdan.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [struct.core :as st]
   [clojure.java.io :as io]))


(defn home-page [{:keys [flash] :as request}]
  (layout/render request "home.html"
                 ;{:docs (-> "docs/docs.md" io/resource slurp)}
                 (merge {:messages (db/get-messages)}
                        (select-keys flash [:name :message :errors]))))

(defn about-page [request]
  (layout/render request "about.html"))

(def message-schema
  [[:name
    st/required
    st/string]
   [:message
    st/required
    st/string
    {:message "message must contain at least 10 characters"
     :validate #(> (count %) 9)}]])

(defn validate-message [params]
  (first (st/validate params message-schema)))

(defn save-message! [params]
  (if-let [errors (validate-message params)]
    (-> (response/found "/")
        (assoc :flash (assoc params :error errors)))
    (do
      (db/save-message!
       (assoc params :timestamp (java.util.Date.)))
      (response/found "/"))))


(defn home-routes []
  [ "" 
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page
         :post save-message!}]
   ["/about" {:get about-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])


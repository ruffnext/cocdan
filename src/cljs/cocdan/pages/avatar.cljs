(ns cocdan.pages.avatar 
  (:require
   [cocdan.db :refer [request-eid-if-not-exist db pull-eid]]))

(defn page
  [{id :id}]
  (let [avatar-id (js/parseInt id)
        eid (request-eid-if-not-exist @db :avatar avatar-id)
        avatar (pull-eid db eid)]
    (when eid
      (js/console.log avatar)
      [:div.container>section.section
       [:h1 (str "AVATAR：" (:name avatar))]])))

(ns cocdan.pages.avatar)

(defn page
  [{id :id}]
  (js/console.log id)
  [:div.container>section.section
   [:h1 (str "AVATAR" id)]])

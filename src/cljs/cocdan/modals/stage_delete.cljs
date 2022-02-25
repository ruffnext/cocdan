(ns cocdan.modals.stage-delete 
  (:require [reagent.core :as r]
            [cocdan.auxiliary :as aux]
            [clojure.core.async :refer [go <!]]
            [cljs-http.client :as http]
            [re-frame.core :as rf]))

(defonce active? (r/atom false))
(defonce stage (r/atom nil))
(defonce delete-status (r/atom "is-danger"))

(aux/init-page
 {}
 {:event/modal-stage-delete-active (fn [_ _ res]
                                     (swap! active? not)
                                     (reset! stage res)
                                     (reset! delete-status "is-danger")
                                     {})})

(defn- close-modal
  []
  (reset! active? false))

(defn- delete-stage
  []
  (reset! delete-status "is-loading")
  (go (let [res  (<! (http/delete (str "/api/stage/s" (:id @stage))))]
        (if (= 204 (:status res))
          (do
            (reset! delete-status "is-danger")
            (rf/dispatch [:event/refresh-my-avatars])
            (js/setTimeout #(reset! active? false) 1000))
          (reset! delete-status "is-danger")))))

(defn stage-delete
  []
  [:div.modal {:class (if @active? "is-active" "")}
   [:div.modal-background {:on-click close-modal}]
   [:div.modal-card
    [:header.modal-card-head
     [:p {:style {:width "100%"}} "DELETE STAGE"]
     [:button.delete {:on-click close-modal}]]
    [:section.modal-card-body
     [:div.field
      [:p "are you sure you want to delete stage " [:strong (:title @stage)] " ?"]
      [:div.content {:style {:padding-left "1.2em"}}]]]
    [:footer.modal-card-foot
     [:button.button {:on-click delete-stage :class @delete-status} "YES I'M SURE"]
     [:button.button {:on-click close-modal} "NO"]]
    [:button {:class "modal-close is-large" :on-click close-modal}]]])
(ns cocdan.event
  (:require [re-frame.core :as rf]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]))

(rf/reg-event-db
 :common/navigate
 (fn [db [_ match]]
   (let [old-match (:common/route db)
         new-match (assoc match :controllers
                          (rfc/apply-controllers (:controllers old-match) match))]
     (assoc db :common/route new-match))))

(rf/reg-fx
 :common/navigate-fx!
 (fn [[k & [params query]]]
   (rfe/push-state k params query)))

(rf/reg-event-fx
 :common/navigate!
 (fn [_ [_ url-key params query]]
   {:common/navigate-fx! [url-key params query]}))

(rf/reg-sub
 :common/page
 :<- [:common/route]
 (fn [route _]
   {:page (-> route :data :view)
    :params (-> route :path-params)}))

(rf/reg-sub
 :common/route
 (fn [db _]
   (-> db :common/route)))

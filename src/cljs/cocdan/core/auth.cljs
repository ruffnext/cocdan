(ns cocdan.core.auth 
  (:require [cljs-http.client :as http]
            [clojure.core.async :refer [<! go]]
            [cocdan.data.client-ds :refer [to-ds]]
            [cocdan.data.stage :refer [new-stage]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe])) 

(defn- init-login-data
  []
  (go
    (let [{stages-status :status
           stages :body} (<! (http/get "/api/stage/list/"))]
      (when (= stages-status 200)
        (let [res (->> stages (map new-stage))]
          (rf/dispatch [:ds/transact-records (map to-ds res)]))))))

(rf/reg-fx
 :after-login
 (fn [_]
   (init-login-data)))

(rf/reg-event-fx
 :event/auth-login
 (fn [{:keys [db]} [_ user-info]]
   (let [current-fragment @(:last-fragment @rfe/history)]
     (if user-info
       (do
         (when (= current-fragment "/")
           (rfe/push-state :main {:nav "stage.list"}))
         {:db (-> db
                  (assoc :auth/user user-info)
                  (assoc :auth/status true))
          :after-login nil})
       {:db (-> db
                (assoc :auth/status false))}))))

(rf/reg-sub
 :common/user-id
 (fn [db _]
   (:id (:auth/user db))))

(defn try-session-login
  [] 
  (go 
    (let [{:keys [status body]} (<! (http/get "/api/auth"))]
      (case status
        200 (rf/dispatch [:event/auth-login body])
        403 (rf/dispatch [:event/auth-login nil])))))

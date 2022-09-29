(ns cocdan.core.auth 
  (:require [cljs-http.client :as http]
            [clojure.core.async :refer [<! go]]
            [cocdan.data.core :as data-core]
            [cocdan.data.stage :refer [new-stage]] 
            [re-frame.core :as rf]
            [datascript.core :as d]))

(rf/reg-event-fx
 :event/auth-login
 (fn [{:keys [db]} [_ user-info]]
   (when user-info
     {:db (-> db
              (assoc :auth/user user-info)
              (assoc :auth/status true))}
     {:db (-> db
              (assoc :auth/status false))})))

(defn- init-login-data
  []
  (go
    (let [{stages-status :status
           stages :body} (<! (http/get "/api/stage/list/"))]
      (when (= stages-status 200)
        (let [res (->> stages (map new-stage))]
          (rf/dispatch [:event/transact-records (map data-core/to-ds res)]))))))

(comment
  (init-login-data))

(rf/reg-event-fx
 :event/after-login
 (fn [{:keys [db]} & _]
   (init-login-data)
   {}))

(rf/reg-sub
 :sub/auth-user
 (fn [db _]
   (:auth/user db)))

(defn query-user-info
  [ds])

(defn try-session-login
  []
  (go 
    (let [{:keys [status body]} (<! (http/get "/api/auth"))]
      (case status
        200 (rf/dispatch [:event/auth-login body])
        403 (rf/dispatch [:event/auth-login nil])))))
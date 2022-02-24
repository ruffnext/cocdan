(ns cocdan.core.user
  (:require
   [posh.reagent :as p]
   [cljs-http.client :as http]
   [clojure.core.async :refer [go <!]]
   [re-frame.core :as rf]))

(defn posh-my-eid
  [ds]
  (p/q '[:find ?my-eid .
         :where
         [?my-eid :my-info/id _]]
       ds))

(defn- try-session-login
  [db & _r]
  (go
    (let [res (<! (http/get "/api/user/whoami"))]
      (if (= (:status res) 200)
        (do
          (rf/dispatch [:rpevent/upsert :my-info (-> res :body :user)])
          (rf/dispatch [:event/refresh-my-avatars]))
        (js/console.log "Session login failed"))))
  db)

(rf/reg-event-db
 :event/try-session-login try-session-login)
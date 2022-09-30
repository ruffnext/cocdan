(ns cocdan.core.api-posher
  (:require [cljs-http.client :as http]
            [clojure.core.async :refer [<! go]]
            [cocdan.data.avatar :refer [new-avatar]]
            [cocdan.data.core :refer [to-ds]]
            [cocdan.data.stage :refer [new-stage]]
            [cocdan.database.main :refer [db]]
            [datascript.core :as d]
            [posh.reagent :as p]
            [re-frame.core :as rf]))

;; 请求特定的数据，如果本地缓存不存在，则更新缓存

(rf/reg-event-fx
 :api/refresh-avatar-by-id
 (fn [_ [_ avatar-id]]
   (go 
     (let [{:keys [status body]} (<! (http/get (str "/api/avatar/" avatar-id)))]
       (if (= status 200)
         (d/transact! db [(-> body new-avatar to-ds)])
         (d/transact! db [{:avatar/id avatar-id}]))))
   {}))

(rf/reg-event-fx
 :api/refresh-stage-by-id
 (fn [_ [_ stage-id]]
   (go
     (let [{:keys [status body]} (<! (http/get (str "/api/stage/" stage-id)))]
       (if (= status 200)
         (d/transact! db [(-> body new-stage to-ds)])
         (d/transact! db [{:stage/id stage-id}]))))
   {}))

(defn posh-avatar-by-id
  [avatar-id]
  (let [record @(p/pull db '[:avatar/props] [:avatar/id avatar-id])
        avatar (:avatar/props record)]
    (when (nil? (:db/id record))
      (rf/dispatch [:api/refresh-avatar-by-id avatar-id]))
    avatar))

(defn posh-stage-by-id
  [stage-id]
  (let [record @(p/pull db '[:stage/props] [:stage/id stage-id])
        avatar (:stage/props record)]
    (when (nil? (:db/id record))
      (rf/dispatch [:api/refresh-stage-by-id stage-id]))
    avatar))

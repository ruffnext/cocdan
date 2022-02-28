(ns cocdan.core.request
  (:require
   [clojure.core.async :refer [go <!]]
   [cljs-http.client :as http]
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :event/patch-to-server
 (fn [_ [_driven-by col-key attrs]]
   (when (and (contains? #{:avatar :stage} col-key) (not (nil? (:id attrs))))
     (go (let [id (:id attrs)
               url (str "/api/" (name col-key) "/" (first (name col-key)) id)
               res (<! (http/patch url {:json-params attrs}))]
           (when (= (:status res) 200)
             (rf/dispatch [:rpevent/upsert col-key (:body res)])))))
   {}))

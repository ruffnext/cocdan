(ns cocdan.core.request
  (:require
   [clojure.core.async :refer [go <!]]
   [cljs-http.client :as http]
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :event/patch-to-server
 (fn [_ [_driven-by base-key attrs]]
   (js/console.log attrs)
   (when (and (contains? #{:avatar :stage} base-key) (not (nil? (:id attrs))))
     (go (let [id (:id attrs)
               url (str "/api/" (name base-key) "/" (first (name base-key)) id)
               _res (<! (http/patch url {:json-params attrs}))])))
   {}))

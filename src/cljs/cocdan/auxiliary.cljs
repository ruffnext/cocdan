(ns cocdan.auxiliary
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [re-frame.core :as rf]
   [cljs-http.client :as http]))

(defn swap-filter-list-map!
  [xs f transform]
  (let [res (map-indexed (fn [i x]
                           (if (f x)
                             i
                             nil)) xs)
        fres (filter #(not (nil? %)) res)]
    (if (empty? fres)
      (conj xs (transform nil))
      (reduce (fn [a x]
                (assoc-in a [x] (transform (nth xs x)))) xs fres))))

(defn request!
  [method url parameters handle-response]
  (go (let [res  (<! (method url {:json-params parameters}))] (cond
                                                                (nil? handle-response) nil
                                                                :else (handle-response res)))))

(defn init-page
  [subscriptions event-handler]
  (doseq [[sub f] subscriptions]
    (rf/reg-sub sub #(f %)))
  (doseq [[event f] event-handler]
    (rf/reg-event-fx event (fn [app-data [driven-by & event-args]]
                             (apply f app-data driven-by event-args)))))

(defn- handle-update
  [db _ keys {f :filter t :transform} _]
  (assoc-in db keys (filter #(not (nil? %)) (swap-filter-list-map!
                                             (reduce (fn [a f] (f a)) db keys) f t))))

(rf/reg-sub
 :subs/general
 (fn [db [_query-id query-v]]
   (reduce (fn [a x] (x a)) db query-v)))

(rf/reg-sub
 :subs/general-get-avatar-by-id
 (fn [db [_query-id id]]
   (if (= id 0)
     {:id 0 :name "System"}
     (let [res (take 1 (filter #(= (:id %) id) (:avatars db)))]
       (if (empty? res)
         (do
           (go (let [res  (<! (http/get (str "/api/avatar/a" id) {}))]
                 (rf/dispatch [:event/general-list-map-conj [:avatars] (case (:status res)
                                                                         200 (:body res)
                                                                         400 {:id id
                                                                              :failed (:body res)}
                                                                         nil)])))
           nil)
         (first res))))))

(rf/reg-sub
 :subs/general-get-stage-by-id
 (fn [db [_query-id id]]
   (let [res (take 1 (filter #(= (:id %) id) (:stages db)))
         on-failed {:id id :loading-status "is-danger"}]
     (if (empty? res)
       (do
         (go (let [res  (<! (http/get (str "/api/stage/s" id) {}))]
               #(rf/dispatch [:event/general-list-map-conj [:stages] (if (= (:status res) 200)
                                                                       (assoc (:body res) :loading-status "is-done")
                                                                       on-failed)])))
         nil)
       (first res)))))

(defn- get-avatars-by-user-id
  [db id]
  (filter #(= (:controlled_by %) id) (:avatars db)))

(rf/reg-sub
 :subs/general-get-avatars-by-user-id
 (fn [db [_query-id id]]
   (get-avatars-by-user-id db id)))

(rf/reg-sub
 :subs/general-get-my-avatars
 (fn [db [_query-id]]
   (let [my-id (:id (:user db))]
     (get-avatars-by-user-id db my-id))))

(rf/reg-event-db
 :event/general-update-list-map
 (fn [app-data [driven-by & event-args]]
   (apply handle-update app-data driven-by event-args)))

(defn- handle-list-map-conj
  [db _ keys val _]
  (cond
    (or (list? val)
        (vector? val)) (reduce (fn [a x]
                                 (handle-list-map-conj a "" keys x "")) db val)
    :else (let [target-list' (reduce (fn [a f] (f a)) db keys)
                target-list (if (nil? target-list') [] target-list')]
            (assoc-in db keys (swap-filter-list-map! target-list #(= (:id %) (:id val)) (fn [x] (merge x val)))))))

(rf/reg-event-db
 :event/general-list-map-conj
 (fn [db [driven-by & event-args]]
   (apply handle-list-map-conj db driven-by event-args)))

(defn <-json
  [val]
  (js->clj (.parse js/JSON val) :keywordize-keys true))

(defn ->json
  [kv]
  (.stringify js/JSON (clj->js kv)))

(ns cocdan.core.avatar
  (:require
   [posh.reagent :as p]
   [re-frame.core :as rf]
   [clojure.core.async :refer [go <!]]
   [cljs-http.client :as http]))

(defn posh-my-avatars
  [ds]
  (p/q '[:find [?a-eid ...]
         :where
         [?my-eid :my-info/id ?my-id]
         [?a-eid :avatar/controlled_by ?my-id]]
       ds))

(defn posh-current-use-avatar-eid
  [ds stage-id]
  (p/q '[:find ?aeid .
         :in $ ?stage-id
         :where
         [?seid :stage/id ?stage-id]
         [?seid :stage/current-use-avatar ?aid]
         [?aeid :avatar/id ?aid]]
       ds
       stage-id))

(defn posh-avatars-by-stage-id
  [ds stage-id]
  (p/q '[:find [?a-eid ...]
         :in $ ?stage-id
         :where
         [?a-eid :avatar/on_stage ?stage-id]]
       ds
       stage-id))

(defn posh-avatar-by-id
  "return null if avatar does not exist in current context"
  [db avatar-id]
  (p/q '[:find ?a-eid .
         :in $ ?avatar-id
         :where [?a-eid :avatar/id ?avatar-id]]
       db
       avatar-id))

(rf/reg-event-db
 :event/refresh-my-avatars
 (fn [db [_driven-by]]
   (go
     (let [res (<! (http/get "/api/user/avatar/list"))]
       (when (= (:status res) 200)
         (let [avatars (-> res :body)
               stage-ids (-> (reduce (fn [a {on_stage :on_stage}]
                                       (if on_stage
                                         (conj a on_stage)
                                         a))
                                     [] avatars)
                             set)]
           (rf/dispatch [:rpevent/upsert :avatar avatars])
           (doseq [stage-id stage-ids]
             (rf/dispatch [:event/request-stage stage-id]))))))
   db))

(defn get-avatar-attr
  [attrs path]
  (reduce (fn [a f] (f a)) attrs (concat [:attributes] path)))

(defn set-avatar-attr
  [attrs path value]
  (assoc-in attrs (concat [:attributes] path) value))

(defn set-avatar-default-attr
  [attrs path value]
  (let [final-path (concat [:attributes] path)
        val (get-avatar-attr attrs final-path)]
    (assoc-in attrs (concat [:attributes] path) (or val value))))

(defn- handle-formula
  [attrs f]
  (cond
    (vector? f) (let [res (reduce (fn [a x]
                                    (let [res (handle-formula attrs x)]
                                      (when (and a res)
                                        (conj a res)))) [] f)]
                  (when (seq res)
                    (apply (first res) (rest res))))
    (keyword? f) (get-avatar-attr attrs [f])
    :else f))

(defn- attr-formula
  [attrs col-key formula]
  (let [res (handle-formula attrs formula)]
    (if (nil? res)
      attrs
      (set-avatar-attr attrs (if (seqable? col-key) col-key [col-key]) res))))

(defn complete-avatar-attributes
  [_avatar-before avatar-now]
  (-> avatar-now
      (attr-formula :age [quot [- :current-date :birthday] [* 60 60 24 365 1000]])))

(comment
  (complete-avatar-attributes
   {} {:attributes {:birthday 741657600000 :current-date 1646265600000}})
  (apply if [1 2 3])
  
  )


(rf/reg-event-fx
 :event/request-avatar
 (fn [_ [_ avatar-id]]
   (go
     (let [{avatar :body status :status} (<! (http/get (str "/api/avatar/a" avatar-id)))]
       (when (= status 200)
         (rf/dispatch [:rpevent/upsert :avatar avatar]))))
   {}))

(rf/reg-event-fx
 :http-event/create-avatar
 (fn [_ [_ avatar]]
   (go
     (let [{status :status body :body :as v} (<! (http/post "/api/avatar/create" {:json-params avatar}))]
       (js/console.log v)
       (when (= status 201)
         (rf/dispatch [:rpevent/upsert :avatar body]))))
   {}))

(def default-avatar
  {:name ""
   :header "img/avatar.png"
   :attributes {:gender "其他"
                :coc {:attrs {:cthulhu-mythos 0}}}})
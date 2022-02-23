(ns cocdan.shell.db
  (:require
   [datascript.core :as d]))

(def db-schema
  {:action/stage {:db/index true}
   :action/type {:db/index true}
   :action/fact {}
   :action/time {}
   :action/order {:db/index true}})

(defonce db (d/create-conn db-schema))

; TODO: presistent this database

(defn query-max-order-of-stage-action
  [stage-id]
  (d/q '[:find (max ?order) .
         :in $ ?stage-id
         :where
         [?e :action/stage ?stage-id]
         [?e :action/order ?order]]
       @db
       stage-id))

(defn action!
  ([stage-id action-type fact time]
   (let [max-order (+ 1 (or (query-max-order-of-stage-action stage-id) 0))
         action {:action/stage stage-id
                 :action/type action-type
                 :action/fact fact
                 :action/time time
                 :action/order max-order}]
     (d/transact! db [action])))

   ([stage-id action-type fact]
    (action! stage-id action-type fact (.getTime (java.util.Date.)))))

(defn query-stage-action?
  [stage-id order]
  (d/q '[:find (pull ?e [*]) .
         :in $ ?stage-id ?order
         :where 
         [?e :action/stage ?stage-id]
         [?e :action/order ?order]]
       @db
       stage-id
       order))

(defn initialize-stage!
  [{stage-id :id :as stage} avatars]
  (action! stage-id "snapshot" {:avatars avatars
                                :stage stage}))

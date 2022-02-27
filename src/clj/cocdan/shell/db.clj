(ns cocdan.shell.db
  (:require
   [datascript.core :as d]
   [cocdan.avatars.auxiliary :as avatars-aux]
   [cocdan.stages.auxiliary :as stages-aux]
   [cats.core :as m]
   [clojure.tools.logging :as log]))

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

(defn make-snapshot!
  "append a snapshot action to db"
  ([stage-id]
   (m/mlet [avatars (avatars-aux/list-avatars-by-stage? stage-id)
            stage (stages-aux/get-by-id? stage-id)]
           (action! stage-id "snapshot" {:avatars avatars
                                         :stage stage})))
  ([{stage-id :id :as stage} avatars]
   (action! stage-id "snapshot" {:avatars avatars
                                 :stage stage}))) 

(defn query-latest-ctx-eid
  [stage-id]
  (let [ctx-eid (->> (d/q '[:find ?order ?ctx-eid
                            :in $ ?stage-id
                            :where
                            [?ctx-eid :action/type "snapshot"]
                            [?ctx-eid :action/stage ?stage-id]
                            [?ctx-eid :action/order ?order]]
                          @db
                          stage-id)
                     (sort-by first)
                     reverse
                     first
                     second)]
    ctx-eid))

(defn reset-stage-actions!
  [stage-id]
  (let [action-eids (d/q '[:find [?e ...]
                           :in $ ?stage-id
                           :where [?e :action/stage ?stage-id]]
                         @db
                         stage-id)]
    (d/transact! db (vec (map (fn [x] [:db.fn/retractEntity x]) action-eids))))
  )

(comment
  (let [res (d/q '[:find [?e ...]
                   :in $ ?stage-id
                   :where [?e :action/stage ?stage-id]]
                 @db
                 1)]
    (vec (map (fn [x] [:db.fn/retractEntity x]) res)))
  (query-stage-action? 2 4)
  (query-max-order-of-stage-action 2)
  (make-snapshot! 2)
  )
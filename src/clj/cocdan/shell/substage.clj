(ns cocdan.shell.substage 
  (:require [clojure.tools.logging :as log]
            [cats.monad.either :as either]
            [cats.core :as m]
            [cocdan.ws.db :as ws-db]
            [cocdan.shell.db :as s-db]))

(def default-substages
  {:debug {:name "debug"}
   :lobby {:name "lobby"}})

#_{:clj-kondo/ignore [:unused-private-var]}
(defn- shell-substage-cmd
  "return a new substage-id if msg-text is a command"
  [_msg-text]
  ())

(def default-substage
  "lobby")

(defn- get-message-substage-id
  [{avatar-id :avatar} substage-ids channel]
  (m/mlet [avatar (ws-db/pull-avatar-by-id @ws-db/db avatar-id)
           avatar-on-substage (either/right (-> avatar :attributes :substage))
           current-substage-id (if (contains? substage-ids (keyword avatar-on-substage))
                                 (either/right avatar-on-substage)
                                 (do
                                   (ws-db/upsert-db! ws-db/db :avatar (assoc-in avatar [:attributes :substage] default-substage))
                                   (s-db/make-snapshot! (:on_stage avatar))
                                   (either/right default-substage)))]
          (m/return current-substage-id)))

(defn- get-substage-ids-from-substage-dict
  [substages]
  (set (reduce (fn [a [k _x]] (conj a k)) [] substages)))

(defn substage
  [message channel]
  (m/mlet [stage (ws-db/pull-stage-by-channel @ws-db/db channel)
           substages (let [substages (-> stage :attributes :substages)]
                       (cond (or (nil? substages) (empty? substages) (empty? (get-substage-ids-from-substage-dict substages)))
                             (do (ws-db/upsert-db! ws-db/db :stage (assoc-in stage [:attributes :substages] default-substages))
                                 (s-db/make-snapshot! (:id stage))
                                 (either/right default-substages))
                             :else (either/right substages)))
           substage-ids (either/right (get-substage-ids-from-substage-dict substages))
           substage-id (get-message-substage-id message substage-ids channel)]
          (either/right (assoc message :substage substage-id))))

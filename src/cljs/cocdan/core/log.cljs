(ns cocdan.core.log
  (:require
   [posh.reagent :as p]
   [datascript.core :as d]
   [cocdan.db :refer [remove-db-perfix]]))

(defn posh-unread-message-count
  [ds avatar-id]
  (p/q '[:find (count ?mids) .
         :in $ ?avatar-id
         :where
         [?avatareid :avatar/id ?avatar-id]
         [?avatareid :avatar/latest-read-message-time ?latest]
         [?mids :message/receiver ?avatar-id]
         [?mids :message/time ?midstime]
         [(> ?midstime ?latest)]]
       ds
       avatar-id))

(defn posh-avatar-latest-message-time
  [ds avatar-id]
  (p/q '[:find (max ?time) .
         :in $ ?avatar-id
         :where
         [?e :message/receiver ?avatar-id]
         [?e :message/time ?time]]
       ds
       avatar-id))

(defn query-latest-messages-by-avatar-id
  [ds avatar-id limit]
  (->> (d/datoms @ds :avet :message/time)
       reverse
       (map :e)
       (reduce (fn [a x]
                 (let [message (d/pull @ds '[*] x)]
                   (if (= (:message/receiver message) avatar-id)
                     (conj a message)
                     a)))
               [])
       (take limit)
       reverse
       (map remove-db-perfix)))
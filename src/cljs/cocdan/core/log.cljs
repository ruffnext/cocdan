(ns cocdan.core.log
  (:require
   [posh.reagent :as p]))

(defn posh-unread-message-eids
  [ds avatar-id]
  (p/q '[:find ?mids
         :in $ ?avatar-id
         :where
         [?avatareid :avatar/id ?avatar-id]
         [?avatareid :avatar/latest-read-message-time ?latest]
         [?mids :message/receiver ?avatar-id]
         [?mids :message/time ?midstime]
         [(> ?midstime ?latest)]]
       ds
       avatar-id))
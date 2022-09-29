(ns cocdan.core.ops 
  (:require [cocdan.data.action :refer [new-play]]
            [cocdan.data.core :as data-core]
            [cocdan.data.stage :refer [new-stage]]
            [cocdan.data.transact :refer [new-transact]]
            [cocdan.core.aux :refer [query-ctx]]
            [datascript.core :as d]))

(def OP-SNAPSHOT :snapshot)
(def OP-TRANSACTION :transact)
(def OP-PLAY :PLAY)

(defn make-op [id ctx-id time type payload]
  [id ctx-id time type payload])

(defn ctx-run!
  ([db [op-id ctx-id op-time op-type op-payload] ack]
   (let [ds @db
         res (case op-type
               :snapshot [{:context/id op-id
                           :context/props (new-stage op-payload)
                           :context/ack ack}
                          {:transact/id op-id
                           :transact/type :snapshot
                           :transact/ack ack
                           :transact/props (new-stage op-payload)}]
               :transact [{:context/id op-id
                           :context/props (data-core/update' (query-ctx ds ctx-id) op-payload)
                           :context/ack ack}
                          {:transact/id op-id
                           :transact/type :transact
                           :transact/props (new-transact op-id ctx-id op-time op-payload)}]
               :PLAY (->> (new-play op-id ctx-id ds op-time op-payload)
                          (map (fn [x] (assoc x :transact/ack ack)))
                          vec)
               [])]
     (if (seq res)
       (d/transact! db res)
       res)))
  ([db [op-id ctx-id op-time op-type op-payload]]
   (ctx-run! db [op-id ctx-id op-time op-type op-payload] false)))

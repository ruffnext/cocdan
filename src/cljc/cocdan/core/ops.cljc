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
                           :context/time op-time
                           :context/ack ack}
                          {:transaction/id op-id
                           :transaction/type "snapshot"
                           :transaction/time op-time
                           :transaction/ack ack
                           :transaction/props (new-stage op-payload)}]
               :transact [{:context/id op-id
                           :context/props (data-core/update' (query-ctx ds ctx-id) op-payload)
                           :context/time op-time
                           :context/ack ack}
                          {:transaction/id op-id
                           :transaction/type "transact"
                           :transaction/time op-time
                           :transaction/props (new-transact op-id ctx-id op-time op-payload)}]
               :PLAY (->> (new-play op-id ctx-id ds op-time op-payload)
                          (map (fn [x] (assoc x
                                              :transaction/ack ack
                                              :transaction/time op-time)))
                          vec)
               [])]
     (if (seq res)
       (d/transact! db res)
       res)))
  ([db [op-id ctx-id op-time op-type op-payload]]
   (ctx-run! db [op-id ctx-id op-time op-type op-payload] false)))

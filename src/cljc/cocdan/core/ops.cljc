(ns cocdan.core.ops 
  (:require [cocdan.data.action :refer [new-play]]
            [cocdan.data.core :as data-core]
            [cocdan.data.stage :refer [new-stage]]
            [cocdan.data.transact :refer [new-transact]]
            [datascript.core :as d]))

(def OP-SNAPSHOT :SNAPSHOT)
(def OP-TRANSACTION :TRANSACTION)
(def OP-PLAY :PLAY)

(defn op [id ctx-id time type payload]
  [id ctx-id time type payload])

(defn- query-ctx
  [db ctx-id]
  (let [res (d/pull db '[:context/props] [:context/id ctx-id])]
    (if (nil? res)
      (println (str "异常！丢失上下文 ctx-id = " ctx-id))
      (:context/props res))))

(defn ctx-run!
  [db [op-id ctx-id op-time op-type op-payload]]
  (let [res (case op-type
              :SNAPSHOT [{:context/id op-id
                          :context/props (new-stage op-payload)}]
              :TRANSACTION [{:context/id op-id
                             :context/props (data-core/update' (query-ctx @db ctx-id) op-payload)}
                            {:transact/id op-id
                             :transact/type :transact
                             :transact/props (new-transact op-id ctx-id op-time op-payload)}]
              :PLAY (new-play op-id ctx-id db op-time op-payload)
              [])]
    (if (seq res)
      (d/transact! db res)
      res)))

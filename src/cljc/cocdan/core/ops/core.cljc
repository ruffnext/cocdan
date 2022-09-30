(ns cocdan.core.ops.core
  (:require [cocdan.data.action :refer [new-play]]
            [cocdan.data.core :as data-core]
            [cocdan.data.stage :refer [new-stage]]
            [cocdan.data.transact :refer [new-transact]]
            [cocdan.database.ctx-db.core :refer [query-ds-ctx-by-id]]
            [datascript.core :as d]))

(def OP-SNAPSHOT :snapshot)
(def OP-UPDATE :update)
(def OP-PLAY :play)

(defn make-op [id ctx-id time type payload]
  [id ctx-id time type payload])

(defn ctx-run!
  ([db [op-id ctx-id op-time op-type op-payload] ack ctx] 
   ;; ctx 和 ctx-id 的 id 可能不同
   ;; 所有的 :context/xxx 都会被存入后端数据库 context 表中
   ;; 所有的 :transaction/xxx 都会被存入后端数据库 transactions 表中
   (let [res (case op-type
               :snapshot (let [tmp (new-stage op-payload)]
                           [{:context/id op-id
                             :context/props tmp
                             :context/time op-time
                             :context/ack ack}
                            {:transaction/id op-id
                             :transaction/type "snapshot"
                             :transaction/time op-time
                             :transaction/ack ack
                             :transaction/props tmp}])
               :update [{:context/id op-id
                         :context/props (data-core/update' ctx op-payload)
                         :context/time op-time
                         :context/ack ack}
                        {:transaction/id op-id
                         :transaction/type "update"
                         :transaction/time op-time
                         :transaction/ack ack
                         :transaction/props (new-transact op-id ctx-id op-time op-payload)}]
               :play (->> (new-play op-id ctx-id ctx op-time op-payload)
                          (map (fn [x] (assoc x
                                              :transaction/ack ack
                                              :transaction/time op-time)))
                          vec)
               [])]
     (when (seq res)
       (d/transact! db res))
     res))
  ([db [op-id ctx-id op-time op-type op-payload] ack]
   (ctx-run! db [op-id ctx-id op-time op-type op-payload] ack (query-ds-ctx-by-id @db ctx-id)))
  ([db [op-id ctx-id op-time op-type op-payload]]
   (ctx-run! db [op-id ctx-id op-time op-type op-payload] false (query-ds-ctx-by-id @db ctx-id))))

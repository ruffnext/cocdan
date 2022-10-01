(ns cocdan.services.ws.db 
  (:require [cocdan.data.aux :refer [add-db-prefix]]
            [datascript.core :as d]))


; [db/id   :attr              :value                ...]
; [  1     :channel/ws         [Object WebSocket]      ]
; [  1     :channel/stage      1                       ]
; [  1     :channel/user       1                       ]
; [  1     :                                           ]
; [  2     :stage/id           1                       ]
; [  2     :stage/config       {:foo "bar"}            ]
; [  2     :stage/...                                  ]
; [  3     :avatar/id          1                       ]
; [  3     :avatar/on_stage    1                       ]
;

(def db-schema
  {:channel/ws {:db/unique :db.unique/identity}
   :stage/id {:db/unique :db.unique/identity}
   :avatar/id {:db/unique :db.unique/identity}})

(defonce db (d/create-conn db-schema))

(defn upsert-one!
  [table-key ds-records]
  (d/transact! db [(add-db-prefix table-key ds-records)]))

(defn query-all-ws-by-stage-id
  "请求舞台上所有的频道"
  [stage-id]
  (->> (d/q '[:find [?ws ...]
              :in $ ?stage-id
              :where
              [?e :channel/stage ?stage-id]
              [?e :channel/ws ?ws]]
            @db stage-id)))

(defn
  query-channels-by-channel
  "请求频道所在舞台的所有频道"
  [ds channel]
  (->> (d/q '[:find ?channels
              :in $ ?channel
              :where
              [?id :channel/ws ?channel]
              [?id :channel/stage ?stage-id]
              [?cid :channel/stage ?stage-id]
              [?cid :channel/ws ?channels]]
            ds
            channel)
       (reduce into [])))


(comment
  (let [listen-fn (fn [x] (println "A"))
        listen-fn-b (fn [x] (println "B"))
        tmp-db (d/create-conn)]
    (d/listen! tmp-db :a listen-fn)
    (d/listen! tmp-db :a listen-fn-b)
    (d/listen! tmp-db :a listen-fn-b)
    (d/transact! tmp-db [{:tmp/id 1}]))
  )
(ns cocdan.services.ws.aux 
  (:require [clojure.string :as str]
            [jsonista.core :as json]
            [cocdan.auxiliary :refer [get-current-time-string]]
            [cocdan.core.ops.core :as op-core]
            [cocdan.database.ctx-db.core :refer [check-consistency
                                                 query-ds-latest-ctx_id
                                                 query-ds-latest-transaction-id]]))

(defn snapshot-if-not-consistency!
  "如果当前状态与给定状态不一致，则生成一个新的 snapshot"
  [db stage-record]
  (let [ds @db]
    (when-not (check-consistency @db stage-record)
      (let [latest-ctx_id (or (query-ds-latest-ctx_id ds) 0)
            latest-transaction-id (or (query-ds-latest-transaction-id ds) 0)
            snapshot (op-core/make-transaction
                      (inc latest-transaction-id)
                      latest-ctx_id
                      (get-current-time-string)
                      :snapshot stage-record)]
        ;todo transact!
        ))))

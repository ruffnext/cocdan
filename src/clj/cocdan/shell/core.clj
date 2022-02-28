(ns cocdan.shell.core
  (:require
   [datascript.core :as d]
   [clojure.tools.logging :as log]
   [cocdan.shell.db :as s-db]
   [cocdan.stages.auxiliary :as stages-aux]
   [cocdan.ws.db :as ws-db]
   [immutant.web.async :as async]
   [cocdan.auxiliary :as gaux :refer [remove-db-perfix]]))

(defn action-listener
  [report]
  (let [tx-data (-> report :tx-data)
        transact-map (reduce (fn [a x]
                               (assoc a (second x) (nth x 2)))
                             {} tx-data)]
    (when-let [stage-id (:action/stage transact-map)]
      (let [channels (ws-db/query-channels-by-stage-id @ws-db/db stage-id)
            action (remove-db-perfix transact-map)
            response (gaux/->json action)]
        (stages-aux/upsert-an-action! action)
        (doseq [channel channels]
          (async/send! channel response))))))

(d/listen! s-db/db :action action-listener)

;; catch 'query' message
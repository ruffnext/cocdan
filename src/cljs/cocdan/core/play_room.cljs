(ns cocdan.core.play-room
  (:require [cljs-http.client :as http]
            [clojure.core.async :refer [go]]
            [cocdan.core.ops.core :as core-ops]
            [cocdan.data.partial-refresh :refer [IPartialRefresh refresh-key]]
            [cocdan.database.ctx-db.core :as ctx-db]
            [datascript.core :as d]
            [re-frame.core :as rf]))

;; 聊天室的核心

(rf/reg-event-fx
 :play/execute
 (fn [_ [_ stage-id transactions]]

   (if-let [ops-sorted (sort-by first transactions)]

    ;; 执行数据库操作
     (let [ds-db (ctx-db/query-stage-db stage-id)
           partial-refreshs (atom #{})]
       (doseq [{:keys [ctx_id] :as transaction} ops-sorted]
         (let [context (ctx-db/query-ds-ctx-by-id @ds-db ctx_id)
               ds-records (core-ops/ctx-generate-ds stage-id transaction context)]
           (when (seq ds-records)
             (d/transact! ds-db ds-records)
             (doseq [{props :transaction/props} ds-records]
               (when (satisfies? IPartialRefresh props)
                 (swap! partial-refreshs #(apply conj % (refresh-key props))))))))
       {:fx [(concat [:dispatch] (map (fn [x] [:partial-refresh/refresh! x]) @partial-refreshs))]})
     ())))

(rf/reg-event-fx
 :play/execute-one-remotly!
 (fn [_ [_ stage-id transaction]]
   (let [ds-db (ctx-db/query-stage-db stage-id)
         context  (ctx-db/query-ds-latest-ctx @ds-db)
         next-transaction-id (inc (ctx-db/query-ds-latest-transaction-id @ds-db))
         ds-records (core-ops/ctx-generate-ds stage-id (assoc transaction :id next-transaction-id) context)
         partial-refreshs (reduce (fn [a {props :transaction/props}]
                                    (if (satisfies? IPartialRefresh props)
                                      (apply conj a (refresh-key props)) a)) #{} ds-records)]
     (d/transact! ds-db ds-records)
     (go (http/post (str "/api/action/a" stage-id "/transact")
                    {:json-params transaction}))
     {:fx [(concat [:dispatch] (map (fn [x] [:partial-refresh/refresh! x]) partial-refreshs))]})))



(ns cocdan.core.play-room
  (:require [cljs-http.client :as http]
            [clojure.core.async :refer [go]]
            [cocdan.core.ops.core :as core-ops :refer [register-transaction-handler]]
            [cocdan.data.partial-refresh :refer [IPartialRefresh refresh-key]]
            [cocdan.data.transaction.patch :refer [handle-patch-op]]
            [cocdan.data.transaction.speak :refer [handler-speak]]
            [cocdan.database.ctx-db.core :as ctx-db]
            [datascript.core :as d]
            [re-frame.core :as rf]))

;; 聊天室的核心

(defn register-transaction-transformers
  "注册一些钩子函数，处理部分 Transaction
   服务端不会注册这些钩子，因为服务端只存原始数据"
  []
  (register-transaction-handler :speak handler-speak)
  (register-transaction-handler :update handle-patch-op))

(defn query-stage-ds
  [stage-id]
  @(ctx-db/query-stage-db stage-id))

(defn query-stage-ctx-by-id
  [stage-id ctx_id]
  (let [db (ctx-db/query-stage-db stage-id)
        ctx (ctx-db/query-ds-ctx-by-id @db ctx_id)]
    ;; 如果 ctx 为空，则应当向服务器发送请求，请求缺失的上下文
    ctx))

(core-ops/register-find-ctx-by-id query-stage-ctx-by-id)

(rf/reg-event-fx
 :fx/refresh-stage-signal
 (fn [{:keys [db]} [_ stage-id]] 
   {:db (update-in db [:stage (keyword (str stage-id)) :refresh] #(if % (inc %) 1))}))

(rf/reg-event-fx
 :fx/refresh-play-chat-log-signal
 (fn [{:keys [db]} [_ stage-id]]
   {:db (update-in db [:stage (keyword (str stage-id)) :refresh-chat-log] #(if % (inc %) 1))}))

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
 (fn [{:keys [db]} [_ stage-id transaction]]
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


;; play/refresh 是刷新整个 play-room 的钩子，谨慎操作
(rf/reg-sub
 :play/refresh
 (fn [db [_ stage-id]]
   (or (get-in db [:stage (keyword (str stage-id)) :refresh]) 0)))

(rf/reg-sub
 :play/refresh-chat-log
 (fn [db [_ stage-id]]
   (or (get-in db [:stage (keyword (str stage-id)) :refresh-chat-log]) 0)))


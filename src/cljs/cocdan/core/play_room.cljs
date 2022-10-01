(ns cocdan.core.play-room
  (:require [cocdan.aux :refer [remove-db-prefix]]
            [cocdan.core.ops.core :as core-ops :refer [register-transaction-handler]]
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
   (js/console.log "REFRESH!")
   {:db (update-in db [:stage (keyword (str stage-id)) :refresh] #(if % (inc %) 1))}))

(rf/reg-event-fx
 :play/execute
 (fn [{:keys [db]} [_ stage-id transactions] ack]

   (when-let [ops-sorted (sort-by first transactions)]

    ;; 执行数据库操作
     (let [stage-key (keyword (str stage-id))
           ds-db (ctx-db/query-stage-db stage-id)]
       (doseq [{:keys [id ctx_id time type props ack] :as transaction} ops-sorted]
         (let [context (ctx-db/query-ds-ctx-by-id @ds-db ctx_id) 
               ds-records (core-ops/ctx-generate-ds stage-id transaction context)]
           (when (seq ds-records)
             (d/transact! ds-db ds-records))))

     ;; 更新 re-frame 状态
       (let [max-transact-id-path [:stage stage-key :max-transact-id]
             rf-max-transact-id (or (get-in db max-transact-id-path) 0)
             last-transact-id (-> ops-sorted last first)
             new-db (update-in db [:stage stage-key :refresh] #(if % (inc %) 0))]
         (if (> last-transact-id rf-max-transact-id)
           {:db (assoc-in new-db max-transact-id-path last-transact-id)}
           {:db new-db}))))))

(rf/reg-event-fx
 :play/execute-one-remotly!
 (fn [{:keys [db]} [_ stage-id transaction]]
   (let [stage-key (keyword (str stage-id))
         ds-db (ctx-db/query-stage-db stage-id)
         context  (ctx-db/query-ds-latest-ctx @ds-db)
         next-transaction-id (inc (ctx-db/query-ds-latest-transaction-id @ds-db))
         ds-records (core-ops/ctx-generate-ds stage-id (assoc transaction :id next-transaction-id) context)
         max-transact-id-path [:stage stage-key :max-transact-id]] 
     (d/transact! ds-db ds-records)
     {:db (assoc-in db max-transact-id-path next-transaction-id)
      :fx [[:dispatch [:fx/refresh-stage-signal stage-id]]]})))

(rf/reg-sub
 :play/refresh
 (fn [db [_ stage-id]]
   (or (get-in db [:stage (keyword (str stage-id)) :refresh]) 0)))


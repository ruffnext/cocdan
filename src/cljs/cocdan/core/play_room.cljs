(ns cocdan.core.play-room
  (:require [cljs-http.client :as http]
            [clojure.core.async :refer [go]]
            [cocdan.aux :refer [get-current-time-string]]
            [cocdan.core.ops.core :as op-core]
            [cocdan.data.partial-refresh :refer [IPartialRefresh refresh-key]]
            [cocdan.database.ctx-db.core :as ctx-db]
            [re-frame.core :as rf]
            [cats.monad.either :as either]))

;; 聊天室的核心

(defn- get-partial-refresh-from-ds-records
  [ds-records]
  (-> (reduce (fn [a {t-props :payload}]
                (cond
                  t-props (if (satisfies? IPartialRefresh t-props)
                            (concat a (refresh-key t-props))
                            (conj a :play-room))
                  :else a)) [] ds-records)
      set vec))

(rf/reg-event-fx
 :play/execute-many-from-remote
 (fn [{:keys [db]} [_ stage-id transactions]]

   (if-let [ops-sorted (sort-by first transactions)]

    ;; 执行数据库操作
     (let [ds-db (ctx-db/query-stage-db stage-id)
           recent-t-id-from-remote (:id (first ops-sorted))
           latest-t-id-from-local (ctx-db/query-ds-latest-transaction-id @ds-db true)
           partial-refresh (atom [])]
       
       ;; 首先是本地的 transaction 入库，之后发送到服务器，服务器确认后再通过
       ;; execute-many-from-remote 返回。因此正常情况下，服务端返回的 ack
       ;; 应该验证的是数据库中最后的一个未验证项。如果不是，则客户端应向服务端
       ;; 发起同步请求
       (when (not= recent-t-id-from-remote (inc latest-t-id-from-local)) 
         (rf/dispatch [:play/retrieve-recent-logs stage-id]))
       

       (doseq [{:keys [ctx_id] :as transaction} ops-sorted]
         (let [context (ctx-db/query-ds-ctx-by-id @ds-db ctx_id)] 
           (either/branch
            (op-core/ctx-run! transaction context)
            (fn [left]
              (js/console.warn (str left))
              (rf/dispatch [:ui/toast "warning" "指令指令失败" (str left)])
              {})
            (fn [[new-t-record new-c-record]] 
              (ctx-db/insert-transactions ds-db [new-t-record])
              (when new-c-record
                (ctx-db/insert-contexts ds-db [new-c-record]))
              (swap! partial-refresh #(concat % (get-partial-refresh-from-ds-records [new-t-record new-c-record]))))))) 
       {:db (assoc-in db [:play (keyword (str stage-id)) :last-ack-transaction-id] (:id (last ops-sorted)))
        :fx [[:dispatch (vec (concat [:partial-refresh/refresh!] @partial-refresh))]]})
     {})))

(defn- execute-transaction-props-easy!
  [stage-id type payload local-only?] 
  (let [ds-db (ctx-db/query-stage-db stage-id)
        ctx  (ctx-db/query-ds-latest-ctx @ds-db)
        max-tid-verified (ctx-db/query-ds-latest-transaction-id @ds-db true)
        max-tid (ctx-db/query-ds-latest-transaction-id @ds-db)
        do-transact? (= max-tid-verified max-tid) ;; 在进行下一次发送请求前，之前的所有消息都应当 ack
        transaction (op-core/make-transaction-v2 (inc max-tid-verified) (:id ctx) 0 (get-current-time-string) type payload false)] 
    (either/branch
     (op-core/ctx-run! transaction ctx)
     (fn [left]
       (rf/dispatch [:ui/toast "warning" "指令指令失败" (str left)])
       (js/console.warn "指令失败")
       {})
     (fn [[new-t-record new-c-record]]
       (when do-transact?
         (ctx-db/insert-transactions ds-db [new-t-record])
         (when new-c-record 
           (ctx-db/insert-contexts ds-db [new-c-record]))
         (when-not local-only?
           (go (http/post (str "/api/action/s" stage-id "/transact")
                          {:edn-params (into {} transaction)}))))
       {:fx [[:dispatch (vec (concat [:partial-refresh/refresh!] (get-partial-refresh-from-ds-records [new-t-record new-c-record])))]]}))))

(rf/reg-event-fx
 :play/execute-transaction-props-to-remote-easy!
 (fn [_ [_ stage-id type payload local-only?]] 
   (if (seq payload)
     (execute-transaction-props-easy! stage-id type payload (or local-only? false))
     (js/console.warn "空的执行命令！"))))

(rf/reg-event-db
 :play/reset-play-room!
 (fn [db _]
   (assoc-in db [:play] {:avatar-id nil
                         :substage-id "lobby"})))

(rf/reg-sub
 :play-sub/avatar-id
 (fn [db [_]]
   (get-in db [:play :avatar-id])))

(rf/reg-sub
 :play-sub/substage-id
 (fn [db [_]]
   (or (get-in db [:play :substage-id]) "lobby")))

(rf/reg-event-fx
 :play/change-avatar-id!
 (fn [{:keys [db]} [_ avatar-id]]
   {:db (assoc-in db [:play :avatar-id] avatar-id)}))

(rf/reg-event-fx
 :play/change-substage-id!
 (fn [{:keys [db]} [_ substage-id]] 
   {:db (assoc-in db [:play :substage-id] substage-id)}))

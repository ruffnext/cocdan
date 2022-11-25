(ns cocdan.core.play-room
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [cljs-http.client :as http]
            [clojure.core.async :refer [go]]
            [cocdan.aux :refer [get-current-time-string]]
            [cocdan.core.ops.core :as op-core]
            [cocdan.data.partial-refresh :refer [IPartialRefresh refresh-key]]
            [cocdan.database.ctx-db.core :as ctx-db]
            [re-frame.core :as rf]))

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
     (let [stage-db (ctx-db/query-stage-db stage-id)
           first-t-id-from-remote (:id (first ops-sorted))
           latest-t-id-from-local (ctx-db/query-latest-transaction-id stage-db)
           partial-refresh (atom [])]
       
       ;; 首先是本地的 transaction 入库，之后发送到服务器，服务器确认后再通过
       ;; execute-many-from-remote 返回。因此正常情况下，服务端返回的 ack
       ;; 应该验证的是数据库中最后的一个未验证项。如果不是，则客户端应向服务端
       ;; 发起同步请求
       (when (not= first-t-id-from-remote (inc latest-t-id-from-local)) 
         (rf/dispatch [:play/retrieve-logs {:stage-id stage-id}]))
       

       (doseq [{:keys [ctx_id] :as transaction} ops-sorted]
         (let [context (ctx-db/query-context-by-id stage-db ctx_id)] 
           (either/branch
            (op-core/ctx-run! transaction context)
            (fn [left]
              (js/console.warn (str left))
              (rf/dispatch [:ui/toast "warning" "指令指令失败" (str left)])
              {})
            (fn [[new-t-record new-c-record]] 
              (ctx-db/insert-transactions stage-db [new-t-record])
              (when new-c-record
                (ctx-db/insert-contexts stage-db [new-c-record]))
              (swap! partial-refresh #(concat % (get-partial-refresh-from-ds-records [new-t-record new-c-record]))))))) 
       {:db (assoc-in db [:play (keyword (str stage-id)) :last-ack-transaction-id] (:id (last ops-sorted)))
        :fx [[:dispatch (vec (concat [:partial-refresh/refresh!] @partial-refresh))]]})
     {})))

(defn- execute-transaction-props-easy!
  [stage-id type payload local-only?] 
  (let [stage-db (ctx-db/query-stage-db stage-id)

        {:keys [transaction_id ctx_id ctx]} (ctx-db/dispatch! stage-db false)
        max-tid-all (ctx-db/query-latest-transaction-id stage-db)] 
    (if (= (inc max-tid-all) transaction_id)
      
      ;; do post
      (m/mlet
       [transaction (either/right (op-core/make-transaction-v2 transaction_id ctx_id 0 (get-current-time-string) type payload false))
        [new-t-record new-c-record] (op-core/ctx-run! transaction ctx)]
       (ctx-db/insert-transactions stage-db [new-t-record])
       (when new-c-record
         (ctx-db/insert-contexts stage-db [new-c-record]))
       (when-not local-only?
         (go (http/post (str "/api/action/s" stage-id "/transact")
                        {:edn-params (into {} transaction)})))
       (either/right
        {:fx [[:dispatch (vec (concat [:partial-refresh/refresh!] (get-partial-refresh-from-ds-records [new-t-record new-c-record])))]]}))
      
      ;; do sync
      (either/left
       "与服务器失去同步"))
    ))

(rf/reg-event-fx
 :play/execute-transaction-props-to-remote-easy!
 (fn [_ [_ stage-id type payload local-only?]]
   (either/branch
    (m/mlet
     [_check-payload (if (seq payload) (either/right) (either/left "指令不能为空"))]
     (execute-transaction-props-easy! stage-id type payload (or local-only? false)))
    (fn [left]
      (rf/dispatch [:ui/toast "执行指令失败，因为" (str left)]))
    (fn [right]
      right))))

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

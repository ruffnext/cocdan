(ns cocdan.core.play-room
  (:require [cljs-http.client :as http]
            [clojure.core.async :refer [go]]
            [cocdan.aux :refer [get-current-time-string]]
            [cocdan.core.ops.core :as op-core]
            [cocdan.data.partial-refresh :refer [IPartialRefresh refresh-key]]
            [cocdan.database.ctx-db.core :as ctx-db]
            [datascript.core :as d]
            [re-frame.core :as rf]
            [cats.monad.either :as either]))

;; 聊天室的核心

(defn- get-partial-refresh-from-ds-records
  [ds-records]
  (-> (reduce (fn [a {t-props :transaction/props c-id :context/id}]
                (cond
                  t-props (if (satisfies? IPartialRefresh t-props)
                            (concat a (refresh-key t-props))
                            (conj a :play-room))
                  c-id (conj a :context)
                  :else a)) [] ds-records)
      set vec))

(rf/reg-event-fx
 :play/execute-many
 (fn [_ [_ stage-id transactions]]

   (if-let [ops-sorted (sort-by first transactions)]

    ;; 执行数据库操作
     (let [ds-db (ctx-db/query-stage-db stage-id)
           partial-refresh (atom [])]
       (doseq [{:keys [ctx_id] :as transaction} ops-sorted]
         (let [context (ctx-db/query-ds-ctx-by-id @ds-db ctx_id)]
           (either/branch
            (op-core/ctx-generate-ds stage-id transaction context)
            (fn [left]
              (js/console.warn (str left))
              (rf/dispatch [:ui/toast "warning" "指令指令失败" (str left)])
              {})
            (fn [ds-records]
              (d/transact! ds-db ds-records)
              (swap! partial-refresh #(concat % (get-partial-refresh-from-ds-records ds-records)))))))
       {:fx [[:dispatch (vec (concat [:partial-refresh/refresh!] @partial-refresh))]]})
     ())))

(defn- execute-transaction-props-easy!
  [stage-id type props local-only?]
  (let [ds-db (ctx-db/query-stage-db stage-id)
        ctx  (ctx-db/query-ds-latest-ctx @ds-db)
        next-tid (inc (ctx-db/query-ds-latest-transaction-id @ds-db true))
        current-max-tid (ctx-db/query-ds-latest-transaction-id @ds-db)
        do-transact? (> next-tid current-max-tid)
        transaction (op-core/make-transaction next-tid (:context/id ctx) 0 (get-current-time-string) type props false)]
    (either/branch
     (op-core/ctx-generate-ds stage-id transaction ctx)
     (fn [left]
       (rf/dispatch [:ui/toast "warning" "指令指令失败" (str left)])
       {})
     (fn [ds-records]
       (when do-transact?
         (d/transact! ds-db ds-records)
         (when-not local-only?
           (go (http/post (str "/api/action/s" stage-id "/transact")
                          {:edn-params (into {} transaction)}))))
       {:fx [[:dispatch (vec (concat [:partial-refresh/refresh!] (get-partial-refresh-from-ds-records ds-records)))]]}))))

(rf/reg-event-fx
 :play/execute-transaction-props-easy!
 (fn [_ [_ stage-id type props local-only?]] 
   (if (seq props)
     (execute-transaction-props-easy! stage-id type props (or local-only? false))
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
   {:db (assoc-in db [:play :avatar-id] avatar-id)
    :fx [[:dispatch [:chat-log/clear-cache!]]]}))

(rf/reg-event-fx
 :play/change-substage-id!
 (fn [{:keys [db]} [_ substage-id]] 
   {:db (assoc-in db [:play :substage-id] substage-id)
    :fx [[:dispatch [:chat-log/clear-cache!]]]}))

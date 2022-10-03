(ns cocdan.core.play-room
  (:require [cljs-http.client :as http]
            [clojure.core.async :refer [go]]
            [cocdan.aux :refer [get-current-time-string]]
            [cocdan.core.ops.core :as op-core]
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
               ds-records (op-core/ctx-generate-ds stage-id transaction context)]
           (when (seq ds-records)
             (d/transact! ds-db ds-records)
             (doseq [{props :transaction/props} ds-records]
               (when (satisfies? IPartialRefresh props)
                 (swap! partial-refreshs #(apply conj % (refresh-key props))))))))
       {:fx [(concat [:dispatch] (map (fn [x] [:partial-refresh/refresh! x]) @partial-refreshs))]})
     ())))

(defn- execute-transaction-props-easy!
  [stage-id type props local-only?]
  (let [ds-db (ctx-db/query-stage-db stage-id)
        ctx  (ctx-db/query-ds-latest-ctx @ds-db)
        next-tid (inc (ctx-db/query-ds-latest-transaction-id @ds-db))
        transaction (op-core/make-transaction next-tid (:context/id ctx) (get-current-time-string) type props false)
        ds-records (op-core/ctx-generate-ds stage-id transaction ctx)
        partial-refreshs (reduce (fn [a {props :transaction/props}]
                                   (if (satisfies? IPartialRefresh props)
                                     (apply conj a (refresh-key props)) a)) #{} ds-records)] 
    (js/console.log ds-records)
    (d/transact! ds-db ds-records)
    (when-not local-only?
      (go (http/post (str "/api/action/a" stage-id "/transact")
                     {:edn-params (into {} transaction)}))) 
    (if (seq partial-refreshs)
      {:fx [[:dispatch (vec (concat [:partial-refresh/refresh!] partial-refreshs))]]}
      {})))

(rf/reg-event-fx
 :play/execute-transaction-props-easy!
 (fn [_ [_ stage-id type props local-only?]] 
   (if (seq props)
     (execute-transaction-props-easy! stage-id type props (or local-only? false))
     (js/console.warn "空的执行命令！"))))

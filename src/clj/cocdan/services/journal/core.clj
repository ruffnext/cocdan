(ns cocdan.services.journal.core 
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [cocdan.auxiliary :refer [get-current-time-string]]
            [cocdan.core.ops.core :as op-core]
            [cocdan.database.ctx-db.core :as ctx-db]
            [cocdan.db.monad-db :as monad-db]
            [cocdan.services.journal.db :as journal-db :refer [update-ctx!]]
            [datascript.core :as d]
            [cocdan.hooks :as hooks]))

;; (defn restore-latest-ctx-from-backend!
;;   "从后端数据库中将持久化的历史记录载入到前端数据库中
;;    载入的范围是上一个 snapshot 到当前最新的记录
;;    同时返回最新的 ctx 的 id"
;;   [stage-id stage-db]
;;   (m/mlet
;;    [latest-ctx-id (monad-db/get-stage-latest-ctx-id-by-stage-id stage-id)
;;     ops (monad-db/list-stage-transactions-after-n stage-id latest-ctx-id)]
;;    (doseq [op (reverse ops)]
;;      (op-core/ctx-run! stage-db op true))
;;    (either/right latest-ctx-id)))

(defn- transact-inner! 
  [stage type props]
  (let [stage-db (ctx-db/query-stage-db stage)
        {:keys [transaction-id ctx-id ctx]} ((cond    ;; 分配 ctx-id 和 transaction-id
                                               (= type op-core/OP-SNAPSHOT) journal-db/dispatch-new-ctx-id
                                               (= type op-core/OP-UPDATE) journal-db/dispatch-new-ctx-id
                                               :else journal-db/dispatch-new-transaction-id) stage)
        op (op-core/make-op transaction-id ctx-id (get-current-time-string) type props)
        res (op-core/ctx-run! stage-db op true ctx)]
    (doseq [res-item res]
      (when (contains? res-item :context/props)
        (update-ctx! stage (:context/props res-item))))
    (either/right res)))

(defn transact!
  [stage type props access-user-id]
  (m/mlet
   [avatars (monad-db/get-avatars-by-user-id access-user-id)
    _check-user-access (if (seq (filter (fn [{stage-id :stage}] (= stage stage-id)) avatars))
                         (either/right)
                         (either/left (str "用户 " access-user-id " 无权在舞台 " stage " 上表演")))]
   (transact-inner! stage type props)))

(defn list-transactions
  [stage-id begin-id limit]
  (m/mlet
   [_stage (monad-db/get-stage-by-id stage-id)]
   (monad-db/list-stage-transactions-after-n stage-id begin-id limit)))

(defn- hook-stage-create
  [stage]
  (transact-inner! (:id stage) op-core/OP-SNAPSHOT stage))

(hooks/hook! :event/after-stage-created hook-stage-create)

(comment
  (let [db (d/create-conn)]
    (d/transact! db [{:db/id 2 :name "a"}])
    (d/transact! db [{:name "b"}])
    @db)
  )
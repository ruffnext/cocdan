(ns cocdan.system-init
  (:require [cats.monad.either :as either]
            [cocdan.core.ops.core :refer [register-context-handler
                                          register-find-ctx-by-id
                                          register-transaction-handler]]
            [cocdan.core.settings :refer [init-default-settings]]
            [cocdan.core.transaction-handler :refer [handle-dice-transaction
                                                     handle-update-context
                                                     handle-snapshot-context
                                                     handle-update-transaction]]
            [cocdan.data.transaction.dice :refer [handle-sc-context
                                                  handle-sc-transaction
                                                  handle-st-context]]
            [cocdan.data.transaction.speak :refer [handle-narration-transaction handler-speak-transaction]]
            [cocdan.database.ctx-db.core :as ctx-db]))

(declare query-stage-ctx-by-id)

;; 注册 Transaction 的处理函数
;;
;; 不同类型的的 Transaction 在客户端需要被预处理为某些状态
;; 从而方便 UI 调用。而服务端由于不进行任何形式的渲染，因此
;; 对于服务端来说，这部分逻辑是不必要的。
(register-context-handler :snapshot handle-snapshot-context)
(register-transaction-handler :speak handler-speak-transaction)
(register-context-handler :update handle-update-context)
(register-transaction-handler :update handle-update-transaction)
(register-transaction-handler :rc handle-dice-transaction)
(register-transaction-handler :ra handle-dice-transaction)
(register-context-handler :st handle-st-context)
(register-transaction-handler :narration handle-narration-transaction)
(register-transaction-handler :sc handle-sc-transaction)
(register-context-handler :sc handle-sc-context)

;; 注册当 context 找不到时的操作
;;
;; 对于服务端而言，这个操作是去数据库中检索
;; 而对于客户端而言，是向服务端发起请求
(register-find-ctx-by-id query-stage-ctx-by-id)

;; 初始化设置系统
(init-default-settings)


;; ============== 函数定义 ==============

(defn query-stage-ctx-by-id
  [stage-id ctx_id]
  (let [db (ctx-db/query-stage-db stage-id)
        ctx (ctx-db/query-ds-ctx-by-id @db ctx_id)]
    ;; 如果 ctx 为空，则应当向服务器发送请求，请求缺失的上下文
    (if ctx
      (either/right ctx)
      (either/left (str "无法找到舞台" stage-id "的上下文id=" ctx_id)))))

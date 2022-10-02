(ns cocdan.system-init 
  (:require [cocdan.core.ops.core :refer [register-find-ctx-by-id
                                          register-transaction-handler]]
            [cocdan.data.transaction.patch :refer [handle-patch-op]]
            [cocdan.data.transaction.speak :refer [handler-speak]]
            [cocdan.database.ctx-db.core :as ctx-db]))

(declare query-stage-ctx-by-id)

;; 注册 Transaction 的处理函数
;;
;; 不同类型的的 Transaction 在客户端需要被预处理为某些状态
;; 从而方便 UI 调用。而服务端由于不进行任何形式的渲染，因此
;; 对于服务端来说，这部分逻辑是不必要的。
(register-transaction-handler :speak handler-speak)
(register-transaction-handler :update handle-patch-op)

;; 注册当 context 找不到时的操作
;;
;; 对于服务端而言，这个操作是去数据库中检索
;; 而对于客户端而言，是向服务端发起请求
(register-find-ctx-by-id query-stage-ctx-by-id)


;; ============== 函数定义 ==============

(defn query-stage-ctx-by-id
  [stage-id ctx_id]
  (let [db (ctx-db/query-stage-db stage-id)
        ctx (ctx-db/query-ds-ctx-by-id @db ctx_id)]
    ;; 如果 ctx 为空，则应当向服务器发送请求，请求缺失的上下文
    ctx))
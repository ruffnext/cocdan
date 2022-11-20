(ns cocdan.system-init
  (:require [cocdan.core.ops.core :refer [register-context-handler
                                          register-transaction-handler]]
            [cocdan.core.settings :refer [init-default-settings]]
            [cocdan.core.transaction-handler :refer [handle-dice-transaction
                                                     handle-update-context
                                                     handle-snapshot-context
                                                     handle-update-transaction]]
            [cocdan.data.transaction.dice :refer [handle-sc-context
                                                  handle-sc-transaction
                                                  handle-st-context]]
            [cocdan.data.transaction.speak :refer [handle-narration-transaction handler-speak-transaction]]))

;; 注册 Transaction 的处理函数
;;
;; 不同类型的的 Transaction 在客户端需要被预处理为某些状态
;; 从而方便 UI 调用。而服务端由于不进行任何形式的渲染，因此
;; 对于服务端来说，这部分逻辑是不必要的。
(register-context-handler :snapshot handle-snapshot-context)           ; refined
(register-transaction-handler :speak handler-speak-transaction)        ; refined
(register-context-handler :update handle-update-context)               ; refined
(register-transaction-handler :update handle-update-transaction)       ; refined
(register-transaction-handler :rc handle-dice-transaction)             ; refined
(register-transaction-handler :ra handle-dice-transaction)             ; refined
(register-context-handler :st handle-st-context)                       ; refined
(register-transaction-handler :narration handle-narration-transaction) ; refined
(register-transaction-handler :sc handle-sc-transaction)               ; refined
(register-context-handler :sc handle-sc-context)                       ; refined

;; 初始化设置系统
(init-default-settings)

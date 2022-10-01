(ns cocdan.data.transaction.core 
  (:require [cocdan.aux :as data-aux]))

;; Transaction 类型
(defprotocol ITransaction
  (get-tid [this])
  (get-time [this])
  (get-ctx_id [this])
  (get-type [this])
  (get-payload [this])
  (to-ds [this]))

(defrecord Transaction [id ctx_id time type props]
  ITransaction
  (get-tid [_this] id)
  (get-time [_this] time)
  (get-ctx_id [_this] ctx_id)
  (get-type [_this] type)
  (get-payload [_this] props)
  (to-ds [this] (data-aux/add-db-prefix :transaction this)))


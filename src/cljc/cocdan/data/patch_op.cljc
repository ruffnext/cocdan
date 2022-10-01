(ns cocdan.data.patch-op
  (:require [cocdan.data.core :refer [ITransaction]] 
            [cocdan.core.ops.core :as op-core]))

(defrecord PatchOP [id ctx_id time ops]
  ITransaction
  (get-tid [_this] id)
  (get-ctx_id [_this] ctx_id)
  (get-time [_this] time))

(defn new-patch-op [id ctx_id time ops] 
  (PatchOP. id ctx_id time ops))

(defn handle-patch-op 
  [_ctx {:keys [id ctx_id time props]}]
  (new-patch-op id ctx_id time props))

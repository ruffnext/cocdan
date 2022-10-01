(ns cocdan.data.transaction.patch)

(defrecord TPatch [ops])

(defn new-patch-op [ops]
  (TPatch. ops))

(defn handle-patch-op
  [_ctx {:keys [props]}]
  (new-patch-op props))

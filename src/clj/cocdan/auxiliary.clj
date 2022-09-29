(ns cocdan.auxiliary)

(defn get-last-insert-id
  [ret-val]
  (-> ret-val first last))
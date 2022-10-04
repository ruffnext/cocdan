(ns cocdan.core.transaction-handler 
  (:require [cocdan.data.transaction.dice :as t-dice]))

(defn handle-dice
  [{_ctx :context/props} {{:keys [avatar attr attr-val dice-result]} :props type :type}]
  (case type
    "rc" (t-dice/->RC avatar attr attr-val dice-result)
    "ra" (t-dice/->RA avatar attr attr-val dice-result)))

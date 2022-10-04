(ns cocdan.core.transaction-handler 
  (:require [cocdan.data.performer.core :refer [get-attr]]
            [cocdan.data.transaction.dice :refer [RC ST]]))

(defn handle-rc
  [{ctx :context/props} {{:keys [avatar attr dice-result]} :props}]
  (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])
        attr-val (get-attr avatar-record attr)]
    (RC. avatar attr attr-val dice-result)))

(defn handle-st
  [_ {{:keys [avatar attr-map]} :props}]
  (ST. avatar attr-map))

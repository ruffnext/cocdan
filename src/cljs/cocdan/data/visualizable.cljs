(ns cocdan.data.visualizable
  (:require [cocdan.data.transaction.patch :refer [TPatch]]))

(defprotocol IVisualizable
  (to-hiccup [this ctx kwargs]))

(extend-type TPatch
  IVisualizable
  (to-hiccup [{:keys [ops]} _ctx _kwargs] 
    [:div
     (map-indexed (fn [i [k before after]]
                    (with-meta [:div.is-flex
                                [:p.transact-key (str k " : ")]
                                [:p.transact-value (str before " --> " after)]] {:key i})) ops)]))
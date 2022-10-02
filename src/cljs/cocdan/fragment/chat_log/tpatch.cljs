(ns cocdan.fragment.chat-log.tpatch 
  (:require [cocdan.data.partial-refresh :refer [IPartialRefresh]]
            [cocdan.data.transaction.patch :refer [TPatch]]
            [cocdan.data.visualizable :refer [IVisualizable]]))

(extend-type TPatch
  IVisualizable
  (to-hiccup [{:keys [ops]} _ctx _kwargs]
    [:div
     (map-indexed (fn [i [k before after]]
                    (with-meta [:div.is-flex
                                [:p.transact-key (str k " : ")]
                                [:p.transact-value (str before " --> " after)]] {:key i})) ops)])

  IPartialRefresh
  (refresh-key [_this] [:chat-log :chat-input]))

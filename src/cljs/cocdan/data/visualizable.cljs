(ns cocdan.data.visualizable
  (:require [cocdan.data.transaction.patch :refer [TPatch]]
            [cocdan.data.transaction.speak :refer [Speak]]
            [cocdan.fragment.speak :as speak]))

(defprotocol IVisualizable
  (to-hiccup [this ctx kwargs]))

(extend-type Speak
  IVisualizable
  (to-hiccup [{:keys [avatar] :as this} ctx {:keys [viewpoint transaction]}]
    (let [avatar-record (get-in (:context/props ctx) [:avatars (keyword (str avatar))])]
      (speak/speak this avatar-record (if (= viewpoint avatar) :right :left) (:ack transaction)))))

(extend-type TPatch
  IVisualizable
  (to-hiccup [{:keys [ops]} _ctx _kwargs] 
    [:div
     (map-indexed (fn [i [k before after]]
                    (with-meta [:div.is-flex
                                [:p.transact-key (str k " : ")]
                                [:p.transact-value (str before " --> " after)]] {:key i})) ops)]))
(ns cocdan.data.visualizable 
  (:require [cocdan.data.action :as action]
            [cocdan.data.transact :refer [Transact]]
            [cocdan.fragment.speak :as speak]))

(defprotocol IVisualizable
  (to-hiccup [this ds kwargs]))

(extend-type action/Speak
  IVisualizable
  (to-hiccup [{:keys [avatar] :as this} ds {:keys [viewpoint]}] 
    (let [ctx (action/get-ctx this ds)
          avatar-record (get-in ctx [:avatars (keyword (str avatar))])] 
      (speak/speak this avatar-record (if (= viewpoint avatar) :right :left)))))

(extend-type Transact
  IVisualizable
  (to-hiccup [{:keys [ops]} _ds _kwargs]
    [:div
     (map-indexed (fn [i [k before after]]
                    (with-meta [:div.is-flex
                                [:p.transact-key (str k " : ")]
                                [:p.transact-value (str before " --> " after)]] {:key i})) ops)]))
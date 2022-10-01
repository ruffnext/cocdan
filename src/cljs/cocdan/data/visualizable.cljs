(ns cocdan.data.visualizable 
  (:require [cocdan.data.action :refer [Speak]]
            [cocdan.data.core :refer [get-ctx_id]]
            [cocdan.data.patch-op :refer [PatchOP]]
            [cocdan.database.ctx-db.core :refer [query-ds-ctx-by-id]]
            [cocdan.fragment.speak :as speak]))

(defprotocol IVisualizable
  (to-hiccup [this ds kwargs]))

(extend-type Speak
  IVisualizable
  (to-hiccup [{:keys [avatar] :as this} ds {:keys [viewpoint]}]
    (let [ctx_id (get-ctx_id this)
          ctx (query-ds-ctx-by-id ds ctx_id)
          avatar-record (get-in (:context/props ctx) [:avatars (keyword (str avatar))])] 
      (speak/speak this avatar-record (if (= viewpoint avatar) :right :left)))))

(extend-type PatchOP
  IVisualizable
  (to-hiccup [{:keys [ops]} _ds _kwargs]
    [:div
     (map-indexed (fn [i [k before after]]
                    (with-meta [:div.is-flex
                                [:p.transact-key (str k " : ")]
                                [:p.transact-value (str before " --> " after)]] {:key i})) ops)]))
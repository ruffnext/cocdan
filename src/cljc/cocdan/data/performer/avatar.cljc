(ns cocdan.data.performer.avatar
  (:require [clojure.core :refer [parse-long]]
            [clojure.string :as str] 
            [cocdan.data.mixin.equipment :refer [IEquipmentMixIn]]
            [cocdan.data.mixin.territorial :refer [ITerritorialMixIn]]
            [cocdan.data.performer.core :refer [IPerformer]]))

(defrecord Avatar [id name image description substage controlled_by payload]

  #?(:cljs INamed)
  #?(:cljs (-name [_this] name))
  #?(:cljs (-namespace [_this] nil))

  #?(:clj clojure.lang.Named)
  #?(:clj (getName [_this] name))
  #?(:clj (getNamespace [_this] nil)) 

  IPerformer
  (get-role [_this] (cond
                      (pos-int? id) "avatar"
                      (neg-int? id) "npc"
                      (= 0 id) "KP"))
  (get-attr [_this prop-name] (or ((keyword (str/lower-case (clojure.core/name prop-name))) (:attrs payload)) 0))
  (set-attr [this attr-name attr-val] (assoc-in this [:payload :attrs (keyword (clojure.core/name attr-name))] attr-val))
  (get-attr-max [_this _prop-name] 10)
  (get-header [_this _mood] "/img/warning_clojure.png")
  (get-image [_this] "/img/warning_clojure.png")
  (get-description [_this] description)
  (get-status [_this] nil)

  ITerritorialMixIn
  (get-substage-id [_this] substage)

  IEquipmentMixIn
  (get-equipments [_this slot-key _only-visible?] (or (slot-key (:equipments payload)) []))
  (get-slots [_this] (sort (vec (set (concat (keys (:equipments payload)) [:背包 :左手 :右手]))))))

(defn- aux-equipment-set
  [equipments]
  (->> (map (fn [[k v]]
              [k (set v)])
            equipments)
       (into {})))

(defn new-avatar
  [{:keys [id name image description substage controlled_by payload]}]
  (let [id (if (string? id) (parse-long id) id)]
    (Avatar. id name image description substage controlled_by (assoc payload
                                                                     :equipments
                                                                     (aux-equipment-set (:equipments payload))))))

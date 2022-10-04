(ns cocdan.data.performer.avatar
  (:require [clojure.string :as str]
            [cocdan.data.core :as data-core]
            [cocdan.data.mixin.equipment :refer [IEquipmentMixIn]]
            [cocdan.data.performer.core :refer [IPerformer]]
            [cocdan.data.mixin.territorial :refer [ITerritorialMixIn]]))

(defrecord Avatar [id name image description substage controlled_by props]

  #?(:cljs INamed)
  #?(:cljs (-name [_this] name))
  #?(:cljs (-namespace [_this] nil))

  #?(:clj clojure.lang.Named)
  #?(:clj (getName [_this] name))
  #?(:clj (getNamespace [_this] nil))

  data-core/IIncrementalUpdate
  (data-core/diff' [this before] (data-core/default-diff' this before))
  (data-core/update' [this ops] (data-core/default-update' this ops))

  IPerformer
  (get-role [_this] (cond
                      (pos-int? id) "avatar"
                      (neg-int? id) "npc"
                      (= 0 id) "KP"))
  (get-attr [_this prop-name] (or ((keyword (str/lower-case prop-name)) (:attrs props)) 0))
  (get-attr-max [_this _prop-name] 10)
  (get-header [_this _mood] "/img/warning_clojure.png")
  (get-image [_this] "/img/warning_clojure.png")
  (get-description [_this] description)
  (get-status [_this] nil)

  ITerritorialMixIn
  (get-substage-id [_this] substage)

  IEquipmentMixIn
  (get-equipments [_this slot-key _only-visible?] (or (slot-key (:equipments props)) []))
  (get-slots [_this] (sort (vec (set (concat (keys (:equipments props)) [:背包 :左手 :右手]))))))

(defn- aux-equipment-set
  [equipments]
  (->> (map (fn [[k v]]
              [k (set v)])
            equipments)
       (into {})))

(defn new-avatar
  [{:keys [id name image description substage controlled_by props]}]
  (let [id (if (string? id) (parse-long id) id)]
    (Avatar. id name image description substage controlled_by (assoc props
                                                                     :equipments
                                                                     (aux-equipment-set (:equipments props))))))

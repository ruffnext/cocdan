(ns cocdan.data.stage
  (:require [cocdan.data.avatar :refer [new-avatar]]
            [cocdan.data.core :as data-core]))

(defrecord Stage [id name introduction image substages avatars controller]

  #?(:cljs INamed)
  #?(:cljs (-name [_this] name))
  #?(:cljs (-namespace [_this] nil))

  #?(:clj clojure.lang.Named)
  #?(:clj (getName [_this] name))
  #?(:clj (getNamespace [_this] nil))

  data-core/IIncrementalUpdate
  (data-core/diff' [this before] (data-core/default-diff' this before)) 
  (data-core/update' [this ops] (data-core/default-update' this ops)))

(defrecord SubStage [id name adjacencies props]
  #?(:cljs INamed)
  #?(:cljs (-name [_this] id))
  #?(:cljs (-namespace [_this] nil))

  #?(:clj clojure.lang.Named)
  #?(:clj (getName [_this] id))
  #?(:clj (getNamespace [_this] nil))

  data-core/IIncrementalUpdate
  (data-core/diff' [this before] (data-core/default-diff' this before))
  (data-core/update' [this ops] (data-core/default-update' this ops)))

(defn new-substage
  [{:keys [id name adjacencies props]}]
  (SubStage. id name adjacencies props))

(defn new-stage
  [{:keys [id name introduction image substages avatars controller]}]
  (let [substages (->> (map new-substage substages)
                       (map (fn [x] [(keyword (str (:id x))) x]))
                       (into {}))
        avatars (->> (map new-avatar avatars)
                     (map (fn [x] [(keyword (str (:id x))) x]))
                     (into {}))]
    (Stage. id name introduction image substages avatars controller)))

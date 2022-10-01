(ns cocdan.data.stage
  (:require [cocdan.core.ops.core :refer [register-context-handler]]
            [cocdan.data.avatar :refer [new-avatar]]
            [cocdan.data.core :as data-core]))

(defrecord Stage [id name introduction image substages avatars controlled_by]

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
  (SubStage. id name adjacencies (or props {})))

(defn new-stage
  [{:keys [id name introduction image substages avatars controlled_by]}]
  (let [substages (->> (map (fn [[k v]] (new-substage (assoc v :id (clojure.core/name k)))) substages)
                       (map (fn [x] [(keyword (str (:id x))) x]))
                       (into {}))
        avatars (->> (map (fn [[_k v]] (new-avatar v)) avatars)
                     (map (fn [x] [(keyword (str (:id x))) x]))
                     (into {}))]
    (Stage. id name introduction image substages avatars controlled_by)))

(register-context-handler new-stage)

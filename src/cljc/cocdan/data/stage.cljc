(ns cocdan.data.stage
  (:require [cocdan.data.performer.avatar :refer [new-avatar]]))

(defrecord Stage [id name introduction image substages avatars controlled_by]

  #?(:cljs INamed)
  #?(:cljs (-name [_this] name))
  #?(:cljs (-namespace [_this] nil))

  #?(:clj clojure.lang.Named)
  #?(:clj (getName [_this] name))
  #?(:clj (getNamespace [_this] nil)))

(defrecord SubStage [id name description adjacencies props]
  #?(:cljs INamed)
  #?(:cljs (-name [_this] id))
  #?(:cljs (-namespace [_this] nil))

  #?(:clj clojure.lang.Named)
  #?(:clj (getName [_this] id))
  #?(:clj (getNamespace [_this] nil)))

(defn new-substage
  [{:keys [id name description adjacencies props]}]
  (SubStage. id name description adjacencies (or props {})))

(defn new-stage
  [{:keys [id name introduction image substages avatars controlled_by]}]
  (let [substages (->> (map (fn [[k v]] (new-substage (assoc v :id (clojure.core/name k)))) substages)
                       (map (fn [x] [(keyword (str (:id x))) x]))
                       (into {}))
        avatars (->> (map (fn [[_k v]] (new-avatar v)) avatars)
                     (map (fn [x] [(keyword (str (:id x))) x]))
                     (into {}))]
    (Stage. id name introduction image substages avatars controlled_by)))

(defn new-context
  [context-props]
  (new-stage context-props))

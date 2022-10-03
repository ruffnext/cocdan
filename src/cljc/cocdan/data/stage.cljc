(ns cocdan.data.stage
  (:require [cocdan.core.ops.core :refer [register-context-handler]]
            [cocdan.data.performer.avatar :refer [new-avatar]]
            [cocdan.data.core :as data-core]
            [cocdan.core.ops.core :as op-core]))

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

(defrecord SubStage [id name description adjacencies props]
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

(register-context-handler (keyword op-core/OP-SNAPSHOT) (fn [_ctx {:keys [props]}] (new-stage props)))
(register-context-handler (keyword op-core/OP-UPDATE) (fn [ctx {:keys [props]}] (new-stage (data-core/update' ctx props))))

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

(defrecord SubStage [id name connected-substages props]
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
  [{:keys [id name connected-substages props]}]
  (SubStage. id name connected-substages props))

(defn new-stage
  [{:keys [id name introduction image substages avatars controller]}]
  (let [substages (->> (map new-substage substages)
                       (map (fn [x] [(keyword (str (:id x))) x]))
                       (into {}))
        avatars (->> (map new-avatar avatars)
                     (map (fn [x] [(keyword (str (:id x))) x]))
                     (into {}))]
    (Stage. id name introduction image substages avatars controller)))

(comment
  (let [avatars [{:id "avatar-1" :name "avatar-name" :image nil :description "description" :controlled-by "user-1"}]
        substages [{:id "lobby" :name "substage-name" :connected-substages [] :performers ["avatar-1"] :props {}}]]
    (new-stage {:id "stage-1" :name "stage-name" :introduction "intro" :image nil
                :substages substages :avatars avatars :controller "user-a"}))
  )
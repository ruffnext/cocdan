(ns cocdan.data.avatar 
  (:require [cocdan.data.core :as data-core]
            [cocdan.data.performer :as performer]))

(defrecord Avatar [id name image description substage controlled-by props]

  #?(:cljs INamed)
  #?(:cljs (-name [_this] name))
  #?(:cljs (-namespace [_this] nil))

  #?(:clj clojure.lang.Named)
  #?(:clj (getName [_this] name))
  #?(:clj (getNamespace [_this] nil))

  data-core/IIncrementalUpdate
  (data-core/diff' [this before] (data-core/default-diff' this before))
  (data-core/update' [this ops] (data-core/default-update' this ops))

  performer/IPerformer
  (props [_this prop-name] ((keyword prop-name) props))
  (header [_this _mood] "/img/warning_clojure.png")
  (image [_this] "/img/warning_clojure.png")
  (description [_this] description)
  (status [_this] nil)

  data-core/ITerritorialMixIn
  (get-substage-id [_this] substage))

(defn new-avatar
  [{:keys [id name image description substage controlled-by props]}]
  (Avatar. id name image description substage controlled-by props))
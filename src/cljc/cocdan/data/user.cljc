(ns cocdan.data.user 
  (:require [cocdan.data.core :as data-core]))

(defrecord User [id name image description]

  #?(:cljs INamed)
  #?(:cljs (-name [_this] name))
  #?(:cljs (-namespace [_this] nil))

  #?(:clj clojure.lang.Named)
  #?(:clj (getName [_this] name))
  #?(:clj (getNamespace [_this] nil)))
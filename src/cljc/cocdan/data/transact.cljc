(ns cocdan.data.transact 
  (:require [cocdan.data.action :refer [IAction]]))

(defrecord Transact [id ctx-id time ops]
  IAction
  (get-id [_this] id)
  (get-ctx [_this _ds] nil))

(defn new-transact [id ctx-id time ops] 
  (Transact. id ctx-id time ops))
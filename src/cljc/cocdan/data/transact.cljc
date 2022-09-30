(ns cocdan.data.transact 
  (:require [cocdan.data.action :refer [IAction]]
            [cocdan.database.ctx-db.core :as ctx-db]))

(defrecord Transact [id ctx-id time ops]
  IAction
  (get-id [_this] id)
  (get-ctx [_this ds] (ctx-db/query-ds-ctx-by-id ds ctx-id)))

(defn new-transact [id ctx-id time ops] 
  (Transact. id ctx-id time ops))
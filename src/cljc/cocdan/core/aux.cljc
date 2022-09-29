(ns cocdan.core.aux 
  (:require [datascript.core :as d]))

(defn query-ctx
  "取得上下文。该函数可能会失败！"
  [db ctx-id]
  (let [res (d/pull db '[:context/props] [:context/id ctx-id])]
    (:context/props res)))

(defn query-latest-ctx
  [ds]
  (->> (d/datoms ds :avet :context/id)
       reverse first first
       (d/pull ds '[:context/props])
       :context/props))

(defn query-latest-ctx-id
  [ds]
  (->> (d/datoms ds :avet :context/id)
       reverse first first
       (d/pull ds '[:context/id])
       :context/id))
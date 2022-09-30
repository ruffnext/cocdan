(ns cocdan.auxiliary)

(defn get-db-action-return
  [ret-val]
  (-> ret-val first last))

(defn timestamp-to-date
  [timestamp]
  (new java.util.Date timestamp))

(defn date-to-timestamp
  [date]
  (. (. java.sql.Timestamp valueOf date) getTime))

(defn get-current-time-string
  []
  (.toString (java.time.Instant/now)))

(comment
   (.toString (java.time.Instant/now)))
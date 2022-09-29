(ns cocdan.database.main
  (:require [datascript.core :as d]
            [posh.reagent :as p]
            [cocdan.data.stage :refer [Stage SubStage]] 
            [cocdan.data.avatar :refer [Avatar]]
            [cocdan.database.schemas :refer [main-database-schema]]
            [re-frame.core :as rf]))

(defonce db (d/create-conn main-database-schema))
(p/posh! db)

(rf/reg-event-fx
 :event/transact-records
 (fn [_ [_ records]]
   (js/console.log records)
   (d/transact! db records)
   {}))

(defn init-testing-data
  [] 
  (let [avatars {:avatar-1 (Avatar. "avatar-1" "swz" "" "" "testing avatar" "user-1" {})}
        substages {:lobby (SubStage. "substage-1" "substage name" [] {})}]
    (d/transact!
     db
     [{:stage/id "1"
       :stage/props (Stage.
                     "stage-1"
                     "stage name"
                     "introduction"
                     "/img/warning_clojure.png"
                     substages
                     avatars
                     "user-1")}])))

(comment
  (d/pull @db '[*] [:stage/id 1])
  )
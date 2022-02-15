(ns cocdan.shell.core
  (:require
   [cats.monad.either :as either]
   [cats.core :as m]
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [cocdan.stages.auxiliary :as stages]
   [clojure.data.json :as json]))

(defn
  ^{:matcher #"(?i)[\.。](?<majorcmd>[A-z]+)[ *]*(?<subcmd>(.*))"}
  books
  [{book :majorcmd
    page :subcmdp} {user :user
                    stage :stageId
                    settings :settings
                    response :response}]
  (let [cmd (string/lower-case book)]
    (cond
      (= "help" cmd) (either/left (format "sample help msg for %s" page))
      (= "config" cmd) (either/left (format "waiting for implementing"))
      (= "whoami" cmd) (either/left (format "you are %s" user))
      (= "hello" cmd) (either/left (format "Hello %s" user))
      (= "stage" cmd) (cond
                        (empty? page) (m/mlet [stageId (either/try-either (Integer/parseInt (name stage)))
                                               stage (stages/get-by-id? stageId)]
                                              (either/left (merge response {:msg (json/write-str (merge stage {:settings settings}))
                                                                            :from 0})))
                        :else (either/left "bad request"))
      :else (either/right (str "command " book " not found")))))

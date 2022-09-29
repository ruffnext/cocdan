(ns cocdan.data.stage-test
  (:require [cocdan.data.stage :as stage-data]))

(comment
  (let [a (stage-data/Stage. "id" "name" "introduction" "image" "substages" "controller")]
    a)
  )
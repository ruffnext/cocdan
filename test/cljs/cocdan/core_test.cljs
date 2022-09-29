(ns cocdan.core-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]
             :refer [run-all-tests]] 
            [pjstadig.humane-test-output]
            [cocdan.data.core-test]
            [cocdan.core :as rc]))

(deftest test-home
  (is (= 2 2)))

(comment
  (run-all-tests)
  )
(ns cocdan.data.core-test
  (:require [cljs.test :refer-macros [is deftest testing]] 
            [cocdan.data.core :as data-core]
            [cocdan.data.stage :as stage]))

;; (deftest test-home
;;   (is (= 2 3)))

(deftest ops-test
  (let [data-a {:key-a #{1 2 3}}
        data-b {:key-a #{1 2 4}}
        diffs (data-core/default-diff' data-a data-b)]
    (is (= data-b (data-core/default-update' data-a diffs)))
    diffs))
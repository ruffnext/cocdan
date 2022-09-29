(ns cocdan.data.core-test
  (:require [cljs.test :refer-macros [is deftest testing]] 
            [cocdan.data.core :as data-core]
            [cocdan.data.stage :as stage]))

;; (deftest test-home
;;   (is (= 2 3)))

(deftest test-data-core
  (let [a (stage/->Stage "id" "name" "introduction" "image" "substages" {:avatar-1 "avatar"} #{1 2})
        b (stage/->Stage "id" "name" "introduction" "image" "substages" {} #{2 3})
        d (data-core/diff' b a)]
    (testing "测试 update"
      (is (= a (data-core/update' b d))))))

(comment
  (let [a (stage/->Stage "id" "name" "introduction" "image" "substages" {:avatar-1 "avatar"} #{1 2})
        b (stage/->Stage "id" "name" "introduction" "image" "substages" {} #{2 3})
        d (data-core/diff' b a)]
    (= a (data-core/update' b d)))
  )
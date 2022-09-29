(ns cocdan.core.ops-test
  (:require [clojure.test :refer [deftest is testing]]
            [cocdan.core.ops :as core-ops]
            [cocdan.data.stage :refer [new-stage]]))

;; (deftest ops-test
;;   (let [avatars [{:id "avatar-1" :name "avatar-name" :image nil :description "description" :controlled-by "user-1"}]
;;         substages [{:id "lobby" :name "substage-name" :connected-substages [] :performers ["avatar-1"] :props {}}]
;;         stage {:id "stage-1" :name "stage-name" :introduction "intro" :image nil
;;                :substages substages :avatars avatars :controller "user-a"}

;;         op1 [1 1 core-ops/OP-SNAPSHOT stage]
;;         ctx1 (core-ops/ctx-run nil op1)
;;         ctx1-real {:context/id 1
;;                    :context/props (new-stage stage)}

;;         op2-diffs [[:name "stage-name" "stage-name-modified"]]
;;         op2 [2 2 core-ops/OP-TRANSACTION op2-diffs]
;;         ctx2 (core-ops/ctx-run ctx1-real op2)
;;         ctx2-real {:context/id 2
;;                    :context/props (assoc (:context/props ctx1-real) :name "stage-name-modified")}

;;         op3 [3 3 core-ops/OP-ACTION {:type :speak :avatar "avatar-1" :payload {:message "hello" :props {}}}]]
;;     (testing "basic test"
;;       (is (= ctx1 ctx1-real))
;;       (is (= ctx2 ctx2-real)))
;;     (core-ops/ctx-run ctx2-real op3))) 
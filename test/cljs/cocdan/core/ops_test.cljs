(ns cocdan.core.ops-test
  (:require [clojure.test :refer [deftest is testing]]
            [cocdan.core.ops.core :as op-core]
            [cocdan.data.stage :refer [new-stage]]))

;; (deftest ops-test
;;   (let [avatars [{:id "avatar-1" :name "avatar-name" :image nil :description "description" :controlled-by "user-1"}]
;;         substages [{:id "lobby" :name "substage-name" :adjacencies [] :performers ["avatar-1"] :props {}}]
;;         stage {:id "stage-1" :name "stage-name" :introduction "intro" :image nil
;;                :substages substages :avatars avatars :controller "user-a"}

;;         op1 [1 1 op-core/OP-SNAPSHOT stage]
;;         ctx1 (op-core/ctx-run nil op1)
;;         ctx1-real {:context/id 1
;;                    :context/props (new-stage stage)}

;;         op2-diffs [[:name "stage-name" "stage-name-modified"]]
;;         op2 [2 2 op-core/OP-TRANSACTION op2-diffs]
;;         ctx2 (op-core/ctx-run ctx1-real op2)
;;         ctx2-real {:context/id 2
;;                    :context/props (assoc (:context/props ctx1-real) :name "stage-name-modified")}

;;         op3 [3 3 op-core/OP-ACTION {:type :speak :avatar "avatar-1" :payload {:message "hello" :props {}}}]]
;;     (testing "basic test"
;;       (is (= ctx1 ctx1-real))
;;       (is (= ctx2 ctx2-real)))
;;     (op-core/ctx-run ctx2-real op3))) 

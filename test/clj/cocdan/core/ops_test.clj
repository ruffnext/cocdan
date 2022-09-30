(ns cocdan.core.ops-test 
  (:require [clojure.test :refer [deftest is testing]]
            [cocdan.core.ops.core :as core-ops]
            [cocdan.data.stage :refer [new-stage]]
            [cocdan.database.schemas :refer [play-room-database-schema]]
            [datascript.core :as d]))

(deftest ops-test
  (let [avatars [{:id "avatar-1" :name "avatar-name" :image nil :description "description" :controlled_by "user-1"}]
        substages [{:id "lobby" :name "substage-name" :adjacencies [] :performers ["avatar-1"] :props {}}]
        stage {:id "stage-1" :name "stage-name" :introduction "intro" :image nil
               :substages substages :avatars avatars :controller "user-a"}
        db (d/create-conn play-room-database-schema)

        op1 (core-ops/op 1 0 1 core-ops/OP-SNAPSHOT stage) 
        ctx1 (core-ops/ctx-run! db op1)
        ctx1-real {:context/id 1
                   :context/props (new-stage stage)} 

        op2-diffs [[:avatars.avatar-1.name "avatar-name" "avatar-name-modified"]]
        op2 (core-ops/op 2 1 1 core-ops/OP-UPDATE op2-diffs)
        ctx2 (core-ops/ctx-run! db op2)
        ctx2-real {:context/id 2
                   :context/props (assoc-in (:context/props ctx1-real) [:avatars :avatar-1 :name] "avatar-name-modified")} 

        op3 (core-ops/op 3 2 3 core-ops/OP-PLAY {:type :speak :avatar "avatar-1" :payload {:message "hello" :props {}}})]
    (testing "basic test"
      (is (= ctx1 ctx1-real))
      (is (= ctx2 ctx2-real)))
    (core-ops/ctx-run! db op3)))
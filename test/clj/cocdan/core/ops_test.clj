(ns cocdan.core.ops-test 
  (:require [cats.core :as m]
            [clojure.test :refer [deftest is testing]]
            [clojure.tools.logging :as log]
            [cocdan.core.ops.core :as op-core]
            [cocdan.data.stage :refer [new-stage]]
            [cocdan.database.ctx-db.core :as ctx-db]
            [cocdan.database.schemas :refer [play-room-database-schema]]
            [datascript.core :as d]))

(deftest ops-test
  (let [avatars [{:id "avatar-1" :name "avatar-name" :image nil :description "description" :controlled_by "user-1"}]
        substages [{:id "lobby" :name "substage-name" :adjacencies [] :performers ["avatar-1"] :props {}}]
        stage (new-stage
               {:id "stage-1" :name "stage-name" :introduction "intro" :image nil
                :substages {} :avatars (->> avatars
                                            (map (fn [{id :id :as s}] [(keyword (str id)) s]))
                                            (into {})) :controller "user-a"})
        db (d/create-conn play-room-database-schema)

        op1 (op-core/make-transaction 1 0 1 0 op-core/OP-SNAPSHOT stage)
        _work (d/transact! db (m/extract (op-core/ctx-generate-ds "stage-1" op1 nil)))
        ctx1 (ctx-db/query-ds-latest-ctx @db)
        _ (log/debug ctx1)
        ctx1-real {:context/id 1
                   :context/ack false
                   :context/time 1
                   :context/props (new-stage stage)}

        op2-diffs [[:avatars.avatar-1.name "avatar-name" "avatar-name-modified"]]
        op2 (op-core/make-transaction 2 1 2 0 op-core/OP-UPDATE op2-diffs)
        _work (d/transact! db (m/extract (op-core/ctx-generate-ds "stage-1" op2 ctx1)))
        ctx2 (ctx-db/query-ds-latest-ctx @db)
        ctx2-real {:context/id 2
                   :context/ack false
                   :context/time 2
                   :context/props (assoc-in (:context/props ctx1-real) [:avatars :avatar-1 :name] "avatar-name-modified")}

        op3 (op-core/make-transaction 3 2 3 0 "speak" {:type :speak :avatar "avatar-1" :payload {:message "hello" :props {}}})]
    (testing "basic test"
      (is (= ctx1 ctx1-real))
      (is (= ctx2 ctx2-real)))
    (m/extract (op-core/ctx-generate-ds db op3))))
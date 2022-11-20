(ns cocdan.core.ops-test 
  (:require [cats.core :as m]
            [clojure.test :refer [deftest is testing]]
            [clojure.tools.logging :as log]
            [cocdan.aux :as data-aux]
            [cocdan.core.ops.core :as op-core] 
            [cocdan.data.stage :refer [new-stage]]
            [cocdan.database.ctx-db.core :as ctx-db :refer [cache-database-schema]]
            [datascript.core :as d]))

(deftest ops-test
  (let [current-time-string (data-aux/get-current-time-string)
        avatars [{:id 0 :name "avatar-name" :image nil :description "description" :controlled_by "user-1"}]
        substages [{:id "lobby" :name "substage-name" :adjacencies [] :performers [0] :props {}}]
        stage (new-stage
               {:id "stage-1" :name "stage-name" :introduction "intro" :image nil
                :substages {} :avatars (->> avatars
                                            (map (fn [{id :id :as s}] [(keyword (str id)) s]))
                                            (into {})) :controller "user-a"}) 
        db (d/create-conn cache-database-schema)

        op1 (op-core/make-context-v2 1 current-time-string stage true)
        _work (ctx-db/insert-contexts db [op1])

        ctx1 (ctx-db/query-ds-latest-ctx @db) 
        ctx1-real {:id 1
                   :ack true
                   :time current-time-string
                   :payload (new-stage stage)}

        op2-diffs [[:avatars.0.name "avatar-name" "avatar-name-modified"]]
        op2 (op-core/make-transaction-v2 2 1 0 current-time-string op-core/OP-UPDATE op2-diffs true)
        _work (m/mlet
               [[new-t-record new-c-record] (op-core/ctx-run! op2 ctx1)]
               (ctx-db/insert-contexts db [new-c-record])
               (ctx-db/insert-transactions db [new-t-record]))

        ctx2 (ctx-db/query-ds-latest-ctx @db)
        ctx2-real {:id 2
                   :ack true
                   :time current-time-string
                   :payload (assoc-in (:payload ctx1-real) [:avatars :0 :name] "avatar-name-modified")}]
    (testing "basic test"
      (is (= ctx1 ctx1-real))
      (is (= ctx2 ctx2-real)))))

(comment
  (ops-test) 
  )
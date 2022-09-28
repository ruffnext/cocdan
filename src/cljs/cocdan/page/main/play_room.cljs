(ns cocdan.page.main.play-room 
  (:require [cocdan.core.ops :as core-ops]
            [cocdan.core.play-room :as p-core]
            [cocdan.data.action :as action]
            [cocdan.data.core :refer [get-substage-id ITerritorialMixIn]]
            [cocdan.data.visualizable :as visualizable]
            [datascript.core :as d]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defonce tmp-db (atom nil))

(defn init-testing-data
  [db]
  (let [avatars [{:id "avatar-1" :name "avatar-name" :image nil :description "description" :substage "lobby" :controlled-by "user-1" :props {:str 100}}]
        substages [{:id "lobby" :name "substage-name" :connected-substages [] :props {}}]
        stage {:id "stage-1" :name "stage-name" :introduction "intro" :image nil
               :substages substages :avatars avatars :controller "user-a"}

        op1 (core-ops/op 1 0 1 core-ops/OP-SNAPSHOT stage) 
        op3 (core-ops/op 2 1 2 core-ops/OP-PLAY {:type :speak :avatar "avatar-1" :payload {:message "hello" :props {}}})
        op2 (core-ops/op 3 1 3 core-ops/OP-TRANSACTION [[:avatars.avatar-1.name "avatar-name" "avatar-name-modified"]])
        op4 (core-ops/op 4 3 4 core-ops/OP-PLAY {:type :speak :avatar "avatar-1" :payload {:message "very very very very very very very very very very very very very very very very very very very very very very very very long hello world again" :props {}}})
        op5 (core-ops/op 5 3 4 core-ops/OP-PLAY {:type :speak :avatar "avatar-1" :payload {:message "hello again" :props {}}})]
    (doseq [op [op1 op2 op3 op4 op5]]
      (core-ops/ctx-run! db op)) 
    (reset! tmp-db db)))

(defn page
  []
  (r/with-let
    [stage-id (or @(rf/subscribe [:sub/stage-performing]) "1")
     substage-name "lobby"
     db (p-core/fetch-stage-db stage-id)
     _ (init-testing-data db)] 
    (let [ds @db
          plays (->> (d/datoms ds :avet :transact/id)
                     reverse (map first)
                     (d/pull-many ds [:transact/props])
                     (map :transact/props)
                     (filter (fn [x] 
                               (or
                                (not (implements? ITerritorialMixIn x))
                                (let [substage (get-substage-id x)]
                                  (= substage substage-name)))))
                     (take 10))]
      [:div.container
       {:style {:padding "3em"}}
       (for [p (reverse plays)]
         (with-meta (visualizable/to-hiccup p ds {}) {:key (action/get-id p)}))])))

(comment
  (take 1 (reverse (d/datoms @@tmp-db :avet :transact/id)))
  )
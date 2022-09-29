(ns cocdan.page.main.play-room 
  (:require [cocdan.core.aux :refer [query-latest-ctx]]
            [cocdan.core.ops :as core-ops]
            [cocdan.core.play-room :as p-core] 
            [cocdan.fragment.chat-log :as chat-log]
            [cocdan.fragment.input :as fragment-input] 
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn init-testing-data
  [stage-id]
  (let [avatars [{:id "avatar-1" :name "avatar-name" :image nil :description "description" :substage "lobby" :controlled_by "user-1" :props {:str 100}}
                 {:id "avatar-2" :name "ruff" :image nil :description "description" :substage "lobby" :controlled_by "user-1" :props {:str 100}}]
        substages [{:id "lobby" :name "substage-name" :adjacencies [] :props {}}]
        stage {:id "1" :name "stage-name" :introduction "intro" :image nil
               :substages substages :avatars avatars :controller "user-a"}

        op1 (core-ops/make-op 1 0 1 core-ops/OP-SNAPSHOT stage)
        op3 (core-ops/make-op 2 1 2 core-ops/OP-PLAY {:type :speak :avatar "avatar-1" :payload {:message "hello" :props {}}})
        op2 (core-ops/make-op 3 1 3 core-ops/OP-TRANSACTION [[:avatars.avatar-1.name "avatar-name" "avatar-name-modified"]])
        op4 (core-ops/make-op 4 3 4 core-ops/OP-PLAY {:type :speak :avatar "avatar-1" :payload {:message "very very very very very very very very very very very very very very very very very very very very very very very very long hello world again" :props {}}})
        op5 (core-ops/make-op 5 3 5 core-ops/OP-PLAY {:type :speak :avatar "avatar-1" :payload {:message "hello again" :props {}}})]
    (rf/dispatch-sync [:play/execute stage-id [op1 op2 op3 op4 op5]])))

(defn page
  []
  (r/with-let
    [stage-id (or @(rf/subscribe [:sub/stage-performing]) "1")
     substage-id (r/atom "lobby")
     avatar-id (r/atom nil)
     _ (init-testing-data stage-id)]
    (let [_refresh @(rf/subscribe [:play/refresh stage-id]) 
          ds (p-core/query-stage-db stage-id)
          latest-ctx (query-latest-ctx ds)] 
      [:div.container
       {:style {:padding-top "1em"
                :padding-left "3em"
                :padding-right "3em"}}
       [:p.has-text-centered @substage-id]
       [chat-log/chat-log 
        {:ctx-ds ds
         :substage @substage-id
         :viewpoint @avatar-id}]
       [fragment-input/input 
        {:context latest-ctx 
         :substage @substage-id
         :hook-avatar-change (fn [x] (reset! avatar-id x))}]])))
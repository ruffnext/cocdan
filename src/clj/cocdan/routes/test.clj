(ns cocdan.routes.test 
  (:require [cocdan.services.stage.core :as stage] 
            [cocdan.middleware.monad-api :refer [wrap-monad]]
            [cocdan.services.auth.core :as auth]
            [cocdan.services.avatar.core :as avatar]
            [cats.monad.either :as either]))

;; 导入测试的数据集

(def test-users
  [{:id 1 :username "string" :nickname "string man"}
   {:id 2 :username "ruff" :nickname "handsome man"}])

(def test-avatars
  [{:id 1 :name "初始角色" :image "" :description "初始的角色" :stage 1 :substage "lobby"  :controlled_by 1 :props {:attrs {:str 100} :equipments {:左手 #{} :右手 #{} :背包 #{"智能手机" "钥匙串"}}}}
   {:id 2 :name "第二个角色" :image "" :description "第二个角色" :stage 1 :substage "lobby" :controlled_by 1 :props {:str 150}}
   {:id 3 :name "第三个角色" :image "" :description "由 2 控制" :stage 1 :substage "lobby" :controlled_by 2 :props {:str 200}}])

(def test-stages
  [{:id 1 :name "测试舞台" :introduction "舞台介绍" :image "" 
    :substages {:lobby {:name "大厅" :adjacencies [] :description "这是一个大厅，篝火在大厅的中央舞动着。柴火还剩下很多，周围也没有怪物，看上去非常安全。你可以在里面休息到你想离开为止。"}} 
    :avatars {} :controlled_by 1}])

(def routes
  ["/test"
   {:swagger {:tags ["test"]}}
   ["/init-test-data" {:post {:summary "载入测试用的数据集"
                              :handler (wrap-monad
                                        (fn [_]
                                          (doseq [{:keys [username nickname]} test-users]
                                            (auth/register! username nickname))
                                          (doseq [stage test-stages]
                                            (stage/create-stage! stage (:controlled_by stage)))
                                          (doseq [avatar test-avatars]
                                            (avatar/create-avatar! avatar (:controlled_by avatar)))
                                          (either/right {:message "ok"})))}}]])

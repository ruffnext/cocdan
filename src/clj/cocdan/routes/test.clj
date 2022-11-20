(ns cocdan.routes.test
  (:require [cats.monad.either :as either]
            [cocdan.middleware.monad-api :refer [wrap-monad]]
            [cocdan.services.auth.core :as auth]
            [cocdan.services.avatar.core :as avatar]
            [cocdan.services.stage.core :as stage]))

;; 导入测试的数据集

(def test-users
  [{:id 1 :username "string" :nickname "string man"}
   {:id 2 :username "ruff" :nickname "handsome man"}])

(def test-avatars
  [{:id 1 :name "初始角色" :image "" :description "PC角色" :stage 1 :substage "lobby"  :controlled_by 1 :payload {:attrs {:str 100} :equipments {:左手 #{} :右手 #{} :背包 #{"智能手机" "钥匙串"}}}}])

(def test-npc
  [{:id -1 :name "NPC" :image "" :description "id 为负数的都是 NPC" :substage "lobby" :controlled_by 0 :payload {:str 150}}
   {:id -2 :name "由玩家控制的 NPC" :image "" :description "这也是一个 NPC，您可以将这个角色让渡给某个玩家进行操控，这时候 controlled_by 填写该玩家的 id" :substage "lobby" :controlled_by 1 :payload {:str 200}}])

(def test-stages
  [{:id 1 :name "测试舞台" :introduction "舞台介绍" :image ""
    :substages {:lobby {:name "大厅" :adjacencies [] :description "这是一个大厅，篝火在大厅的中央舞动着。柴火还剩下很多，周围也没有怪物，看上去非常安全。你可以在里面休息到你想离开为止。"}
                :train {:name "列车" :adjacencies [] :description "一辆运行中的列车的车厢，你能感受到这个列车在前进。车厢前半截被柔和的灯光昏暗地照亮，而后半截车厢的照明似乎坏掉了。"}}
    :avatars (into {} (map (fn [{id :id :as x}] [(keyword (str id)) x]) test-npc)) :controlled_by 1}])

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

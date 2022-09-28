(ns cocdan.page.main
  (:require ["@ant-design/icons" :refer [BookOutlined SettingOutlined
                                         TeamOutlined PlayCircleOutlined]]
            ["antd" :refer [Layout Menu]]
            [clojure.string :as s]
            [cocdan.page.main.avatars :as avatars-page]
            [cocdan.page.main.stages :as stages-page]
            [cocdan.page.main.settings :as settings-page]
            [cocdan.page.main.play-room :as play-room]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]))

(rf/reg-event-db
 :event/stage-performing
 (fn [db [_event-key stage-id]] 
   (assoc db :stage/performing stage-id)))

(rf/reg-sub
 :sub/stage-performing
 (fn [db _]
   (-> db :stage/performing)))

(defn- nav-item
  "创建导航栏的辅助函数"
  ([label key icon children]
   {:key key :icon icon :children children :label label})
  ([label key icon]
   {:key key :icon icon :label label})
  ([label key]
   {:key key :label label}))

(def page-router
  {:avatars #'avatars-page/page
   :stages.list #'stages-page/page-list
   :stages.join #'stages-page/page-joined
   :settings.show #'settings-page/page-show
   :performing #'play-room/page})

(defn page
  [{nav-id :nav}]
  (r/with-let
    [collapsed (r/atom false)
     nav (fn [k]
           (rfe/push-state :main {:nav k}))]
    [:> Layout
     {:style {:minHeight "100vh"}}
     [:> (.-Sider Layout)
      {:collapsible true
       :onCollapse #(reset! collapsed %)
       :collapsed @collapsed}
      [:div.logo]
      [:> Menu
       {:onSelect #(->> (.-keyPath %) reverse (s/join ".") keyword nav)
        :theme "dark"
        :mode "inline"
        :selectedKeys [(last (s/split nav-id "."))]
        :items (-> [(nav-item "我的角色" "avatars" (r/as-element [:> TeamOutlined]))
                    (nav-item "演出舞台" "stages" (r/as-element [:> BookOutlined])
                              [(nav-item "舞台列表" "list")
                               (nav-item "检索舞台" "join")])
                    (nav-item "设置" "settings" (r/as-element [:> SettingOutlined])
                              [(nav-item "演出设置" "show")
                               (nav-item "实验配置" "experiment")])]
                   (#(if @(rf/subscribe [:sub/stage-performing])
                       (conj % (nav-item "正在演出" "performing" (r/as-element [:> PlayCircleOutlined])))
                       %)))}]]
     [:> Layout
      {:class "site-layout"}
      [:> (.-Header Layout)]
      [:> (.-Content Layout)
       (let [this-page ((keyword nav-id) page-router)]
         (if this-page [this-page] (nav "stages.list")))]]]))
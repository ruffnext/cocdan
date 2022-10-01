(ns cocdan.page.main
  (:require ["@ant-design/icons" :refer [BookOutlined SettingOutlined
                                         UserOutlined PlayCircleOutlined
                                         EditOutlined IdcardOutlined
                                         AppstoreAddOutlined]]
            ["antd" :refer [Layout Menu]]
            [clojure.string :as s]
            [cocdan.page.main.avatars :as avatars-page]
            [cocdan.page.main.stages :as stages-page]
            [cocdan.page.main.settings :as settings-page]
            [cocdan.page.main.play-room :as play-room]
            [cocdan.page.main.other :as other-page]
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
   :stages.join #'stages-page/page-search
   :stage-edit #'stages-page/page-edit
   :avatar-edit #'avatars-page/page-edit
   :settings.show #'settings-page/page-show
   :other.export #'other-page/export-page
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
      [:div.logo.is-vcentered
       [:p.has-text-white.title.is-4 {:style {:text-align "center"
                                              :padding-top "2px"}} "C O C　団"]]
      [:> Menu
       {:onSelect #(->> (.-keyPath %) reverse (s/join ".") keyword nav)
        :theme "dark"
        :mode "inline"
        :selectedKeys [(last (s/split nav-id "."))]
        :items (-> [(nav-item "我的角色" "avatars" (r/as-element [:> IdcardOutlined]))
                    (nav-item "演出舞台" "stages" (r/as-element [:> BookOutlined])
                              [(nav-item "舞台列表" "list")
                               (nav-item "检索舞台" "join")])
                    (nav-item "其他" "other" (r/as-element [:> AppstoreAddOutlined])
                              [(nav-item "导出日志" "export")])
                    (nav-item "设置" "settings" (r/as-element [:> SettingOutlined])
                              [(nav-item "演出设置" "show")
                               (nav-item "实验配置" "experiment")])
                    (nav-item "车卡" "avatar-edit" (r/as-element [:> UserOutlined]))
                    (nav-item "编辑舞台" "stage-edit" (r/as-element [:> EditOutlined]))]
                   (#(if @(rf/subscribe [:sub/stage-performing])
                       (conj % (nav-item "正在演出" "performing" (r/as-element [:> PlayCircleOutlined])))
                       %)))}]]
     [:> Layout
      {:class "site-layout"}
      [:> (.-Header Layout)]
      [:> (.-Content Layout)
       (let [this-page ((keyword nav-id) page-router)]
         (if this-page [this-page] (nav "stages.list")))]]]))
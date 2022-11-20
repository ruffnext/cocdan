(ns cocdan.core
  {:dev/always true}
  (:require ["react-dom/client" :refer [createRoot]]
            [cocdan.core.auth :as core-auth]
            [cocdan.event]
            [cocdan.modal.core :as modal-core]
            [cocdan.page.login :as login]
            [cocdan.page.main :as main]
            [cocdan.system-init]
            [goog.dom :as gdom]
            [malli.dev.cljs :as md]
            [malli.dev.pretty :as pretty]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reitit.core :as reitit]
            [reitit.frontend.easy :as rfe]))

;; 网站内的路径跳转，例如 http://localhost:3000/#/webui 跳转到 main/page 中
(def router
  (reitit/router
   [["/" {:name        :login
          :view        #'login/page}]
    ["/webui/:nav" {:name :main
                    :view #'main/page}]]))

(defn page []
  (let [{page :page params :params} @(rf/subscribe [:common/page])] 
    (if page
      [:div
       [page params]
       (modal-core/page)]
      [:p "没有匹配页面"])))

;; ===================================== System Init ===================================

(defonce root (r/atom nil))
(defonce login-flag (r/atom false))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(defn start-router! []
  (rfe/start!
   router
   navigate!
   {}))

(defn ^:dev/after-load render-page
  "重新渲染整个页面"
  []
  (md/start! {:report (pretty/thrower)})
  (rf/clear-subscription-cache!) 
  (let [^js/ReactDOMRoot r-root @root]
    (.render r-root (r/as-element [#'page]))))

(defn init!
  "整个网页 app 入口，在 shadow-cljs.edn 中指定。这个函数仅运行一次"
  []
  ;; (init-testing-data) 
  (md/start! {:report (pretty/thrower)})
  (when (nil? @root)
    (reset! root (createRoot (gdom/getElement "app"))))
  (when (not @login-flag)  ; 避免测试的时候登录两次
    (core-auth/try-session-login)
    (swap! login-flag not))
  
  (start-router!)
  (render-page))

(ns cocdan.core
  (:require
   ["react-dom/client" :refer [createRoot]]
   [reagent.core :as r]
   [goog.dom :as gdom]
   [reitit.frontend.easy :as rfe]
   [re-frame.core :as rf]
   [reitit.core :as reitit]
   [cocdan.event] 
   [cocdan.core.auth :as core-auth]
   [cocdan.data.visualizable]
   [cocdan.database.main :refer [init-testing-data]]
   [cocdan.page.login :as login]
   [cocdan.page.main :as main]))

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
      [page params]
      [:p "没有匹配页面"])))

;; ===========================================================================================

(defonce root (r/atom nil))

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
  (rf/clear-subscription-cache!) 
  (let [^js/ReactDOMRoot r-root @root]
    (.render r-root (r/as-element [#'page]))))

(defn init!
  "整个网页 app 入口，在 shadow-cljs.edn 中指定。这个函数仅运行一次"
  []
  ;; (init-testing-data)
  (core-auth/try-session-login)
  (start-router!)
  (when (nil? @root)
    (reset! root (createRoot (gdom/getElement "app"))))
  (render-page))

(comment 
)
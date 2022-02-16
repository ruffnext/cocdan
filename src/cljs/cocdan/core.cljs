(ns cocdan.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [markdown.core :refer [md->html]]
   [cocdan.ajax :as ajax]
   [cocdan.events]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [cocdan.components.console :as c-console]
   [cocdan.pages.login :as login-page]
   [cocdan.pages.user :as user-page]
   [cocdan.pages.bulma :as bulma]
   [cocdan.pages.avatar :as avatar]
   [cocdan.pages.stage :as stage]
   [cocdan.pages.modals :as modals]
   [cocdan.core.indexeddb]
   [cocdan.core.chat]
   [cocdan.core.stage]
   [cocdan.core.request]
   [cocdan.db :refer [defaultDB]])
  #_{:clj-kondo/ignore [:unused-import]}
  (:import goog.History))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "COC 団"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span] [:span] [:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]
       [nav-link "#/about" "About" :about]
       [nav-link "#/cmd" "Command" :cmd]
       [nav-link "#/bulma" "Bulma" :bulma]]
      [:div.navbar-end
       (let [user @(rf/subscribe [:user])]
         (if (empty? user)
           [nav-link "#/login" "Login" :login]
           [nav-link "#/user" (:name user) :user]))]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn home-page []
  [:section.section>div.container>div.content
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn footer
  []
  [:footer.footer {:style {:padding-bottom "1em"}}
   [:div {:class "content has-text-centered"}
    [:p "© 2022 CoC-Dan (temporary) some rights reserved"]]])

(defn page []
  (let [{page :page params :params} @(rf/subscribe [:common/page])]
    (if page
      [:div
       [navbar]
       [page params]
       [modals/page]
       [footer]]
      nil)))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
   [["/" {:name        :home
          :view        #'home-page
          :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
    ["/about" {:name :about
               :view #'about-page}]
    ["/cmd" {:name :cmd
             :view (var c-console/cmd-page)}]
    ["/bulma" {:name :bulma
               :view (var bulma/page)}]
    ["/login" {:name :login
               :view (var login-page/page)}]
    ["/user" {:name :user
              :view (var user-page/page)}]
    ["/avatar/:id" {:name :avatar
                    :view (var avatar/page)}]
    ["/stage/:id" {:name :stage
                   :view (var stage/page)}]]))

(defn start-router! []
  (rfe/start!
   router
   navigate!
   {}))

(defn init-events
  []
  (rf/dispatch [:event/try-session-login]))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init-sys
  [init-db]
  (rf/reg-event-db
   :init-db
   (fn
     [_ _]
     init-db))
  (rf/dispatch-sync [:init-db]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components)
  (init-sys defaultDB)
  (init-events))
(ns cocdan.pages.login
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [cljs-http.client :as http]
            [reitit.frontend.easy :as rfe]
            [reagent.core :as r]
            [cats.monad.either :as either]
            [cats.core :as m]
            [clojure.core.async :refer [go <!]]
            [cocdan.core.user :refer [posh-my-eid]]
            [cocdan.db :as gdb]))

(defn- err-msg
  [msg]
  [:p {:class "help is-danger"} msg])

(defn- login-button
  [username' email']
  (r/with-let [status (r/atom "is-primary")
               on-submit (fn [username' email']
                           (m/mlet [username username'
                                    email email']
                                   (reset! status "is-loading")
                                   (go
                                     (let [res (<! (http/post "/api/user/login" {:json-params {:name username :email email}}))]
                                       (if (= (:status res) 200)
                                         (do
                                           (reset! status "is-done")
                                           (rf/dispatch [:event/try-session-login]))
                                         (reset! status "is-danger"))))))]
    [:button
     {:class (str "button " @status)
      :on-click #(on-submit username' email')}
     "LINK START"]))

(defn- register-button
  [username' email']
  (r/with-let [status (r/atom "")
               on-submit (fn [username' email']
                           (m/mlet [username username'
                                    email email']
                                   (reset! status "is-loading")
                                   (go
                                     (let [register (<! (http/post "/api/user/register" {:json-params {:name username :email email}}))]
                                       (if (= (:status register) 201)
                                         (let [login (<! (http/post "/api/user/login" {:json-params {:name username :email email}}))]
                                           (if (= (:status login) 200)
                                             (rf/dispatch [:event/try-session-login])
                                             (reset! status "is-danger")))
                                         (reset! status "is-danger"))))))]
    [:button
     {:class (str "button " @status)
      :on-click #(on-submit username' email')}
     "Register"]))

(defn- login-page
  []
  (r/with-let [username (r/atom (either/right ""))
               email (r/atom (either/right ""))]
    [:section.section
     [:div {:style {:text-align "center"}}
      [:a {:href "#/about"} [:img {:src "/img/soslog.jpg"}]]
      [:div {:class "is-centered columns is-mobile"}
       [:div {:class "column is-narrow" :style {:margin-top 40}}
        [:div {:class "field is-horizontal"}
         [:div {:class "field-label is-normal"}
          [:label.label {:style {:min-width 100}} "Username"]]
         [:div.field-body>div.field
          [:div {:class "control has-icons-left"}
           [:input {:class (if (either/right? @username) "input" "input is-danger")
                    :type "text"
                    :on-change (fn [x]
                                 (let [val (-> x .-target .-value)]
                                   (if (str/blank? val)
                                     (reset! username (either/left "username is required"))
                                     (reset! username (either/right val)))))}]
           [:span {:class "icon is-small is-left"}
            [:i {:class "fas fa-user"}]]]
          (either/branch @username #(err-msg %) #())]]
        [:div {:class "field is-horizontal"}
         [:div {:class "field-label is-normal"}
          [:label.label {:style {:min-width 100}} "Email"]]
         [:div.field-body>div.field
          [:div {:class "control has-icons-left"}
           [:input {:class (if (either/right? @username) "input" "input is-danger")
                    :type "email"
                    :on-change (fn [x]
                                 (let [val (-> x .-target .-value)]
                                   (if (str/blank? val)
                                     (reset! email (either/left "email is required"))
                                     (reset! email (either/right val)))))}]
           [:span {:class "icon is-small is-left"}
            [:i {:class "fas fa-envelope"}]]]
          (either/branch @email #(err-msg %) #())]]
        [:div {:class "columns is-horizontal"}
         [:div {:class "field-label is-normal"  :style {:min-width 100}}]
         [:div.field-body
          [:div {:class "is-half column"}
           (login-button @username @email)]
          [:div {:class "is-half column"}
           (register-button @username @email)]]]]]]]))

(defn page
  []
  (let [user @(posh-my-eid gdb/db)]
    (if user
      (rfe/push-state :user)
      (login-page))))

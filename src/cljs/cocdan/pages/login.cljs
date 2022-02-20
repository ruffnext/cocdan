(ns cocdan.pages.login
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [cljs-http.client :as http]
            [reitit.frontend.easy :as rfe]
            [cocdan.auxiliary :as aux]))

(defn- set-username
  [{:keys [db]} _ username]
  {:db (update-in db [:login] assoc
                  :username username
                  :username-error (if (str/blank? username)
                                    "username is required"
                                    nil))})

(defn- set-email
  [{:keys [db]} _ email]
  {:db (update-in db [:login] assoc
                  :email email
                  :email-error (if (str/blank? email)
                                 "email is required"
                                 nil))})

(defn- on-submit
  [{{{username :username email :email} :login :as db} :db} _ action]
  (let [username-blank (str/blank? username)
        email-blank (str/blank? email)
        username-validate (not username-blank)
        email-validate (not email-blank)
        action (when (and username-validate email-validate)
                    action)
        res (-> {:db db}
                (#(if username-blank
                    (assoc-in % [:db :login :username-error] "username is required")
                    (assoc-in % [:db :login :username-error] "")))
                (#(if email-blank
                    (assoc-in % [:db :login :email-error] "email address is required")
                    (assoc-in % [:db :login :email-error] "")))
                (#(case action
                    "login" (assoc % :http-request {:url "/api/user/login"
                                                    :method http/post
                                                    :req-params {:json-params {:name username
                                                                               :email email}}
                                                    :resp-event :event/login})
                    "register" (assoc % :http-request {:url "/api/user/register"
                                                       :method http/post
                                                       :req-params {:json-params {:name username
                                                                                  :email email}}
                                                       :resp-event :event/register})
                    %))
                (#(case action
                    "login" (assoc-in % [:db :login :status] "inprogress")
                    "register" (assoc-in % [:db :login :register-status] "inprogress")
                    %)))]
    res))

(defn- login
  [{:keys [db]} _ res _]
  {:db (-> db
           (#(assoc-in % [:login :status]
                       (case (:status res)
                         400 "failed"
                         200 "done"
                         "default")))
           (#(if (= 200 (:status res))
               (do
                 (rf/dispatch [:event/try-session-login])
                 (assoc % :user (-> res :body :user)))
               %)))})

(defn- register
  [{:keys [db]} _ {status :status body :body} _]
  (case status
    400 (-> {:db db}
            (assoc-in [:db :login :register-status] "failed")
            (assoc-in [:db :login :email-error] (:error body)))
    201 (do
          (rf/dispatch [:event/login-submit "login"])
          (-> {:db db}
              (assoc-in [:db :login :register-status] "done")))
    {}))

(aux/init-page
 {:subs/login-username #(->> % :login :username)
  :subs/login-email    #(->> % :login :email)
  :subs/login-username-error #(->> % :login :username-error)
  :subs/login-email-error #(->> % :login :email-error)
  :subs/login-status #(->> % :login :status)
  :subs/login-register-status #(-> % :login :register-status)}
 {:event/login-username set-username
  :event/login-email    set-email
  :event/login-submit   on-submit
  :event/login-register-click :login   ; TODO : register page
  :event/login login
  :event/register register})

(defn- err-msg
  [msg]
  (if (str/blank? msg)
    nil
    [:p {:class "help is-danger"} msg]))

(defn- login-button
  [status]
  (let [class (case status
                "failed" "button is-danger"
                "inprogress" "button is-loading"
                "button is-primary")
        text (case status
               "failed" "FAILED"
               "inprogress" "Login..."
               "done" "SUCCESS"
               "LINK START")]
    [:button
     {:class class
      :on-click #(rf/dispatch [:event/login-submit "login"])}
     text]))

(defn- register-button
  [status]
  (let [class (case status
                "failed" "button is-danger"
                "inprogress" "button is-loading"
                "button")
        text (case status
               "failed" "FAILED"
               "inprogress" "Registering"
               "done" "SUCCESS"
               "Register")]
    [:button
     {:class class
      :on-click #(rf/dispatch [:event/login-submit "register"])}
     text]))

(defn- login-page
  []
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
         [:input {:class (if (str/blank? @(rf/subscribe [:subs/login-username-error]))
                           "input"
                           "input is-danger")
                  :type "text"
                  :onBlur #(rf/dispatch [:event/login-username (-> % .-target .-value)])}]
         [:span {:class "icon is-small is-left"}
          [:i {:class "fas fa-user"}]]]
        (err-msg @(rf/subscribe [:subs/login-username-error]))]]
      [:div {:class "field is-horizontal"}
       [:div {:class "field-label is-normal"}
        [:label.label {:style {:min-width 100}} "Email"]]
       [:div.field-body>div.field
        [:div {:class "control has-icons-left"}
         [:input {:class (if (str/blank? @(rf/subscribe [:subs/login-email-error]))
                           "input"
                           "input is-danger")
                  :type "email"
                  :onBlur #(rf/dispatch [:event/login-email (-> % .-target .-value)])}]
         [:span {:class "icon is-small is-left"}
          [:i {:class "fas fa-envelope"}]]]
        (err-msg @(rf/subscribe [:subs/login-email-error]))]]
      [:div {:class "columns is-horizontal"}
       [:div {:class "field-label is-normal"  :style {:min-width 100}}]
       [:div.field-body
        [:div {:class "is-half column"}
         (login-button @(rf/subscribe [:subs/login-status]))]
        [:div {:class "is-half column"}
         (register-button @(rf/subscribe [:subs/login-register-status]))]]]]]]])





(defn page
  []
  (let [user @(rf/subscribe [:user])]
    (if (empty? user)
      (login-page)
      (rfe/push-state :user))))

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

(defn- link-start
  [{{{username :username email :email} :login :as db} :db} _]
  (let [username-blank (str/blank? username)
        email-blank (str/blank? email)
        username-validate (not username-blank)
        email-validate (not email-blank)
        do-login? (and username-validate email-validate)
        res (-> {:db db}
                (#(if username-blank
                    (assoc-in % [:db :login :username-error] "username is required")
                    (assoc-in % [:db :login :username-error] "")))
                (#(if email-blank
                    (assoc-in % [:db :login :email-error] "email address is required")
                    (assoc-in % [:db :login :email-error] "")))
                (#(if do-login?
                    (assoc % :http-request {:url "/api/user/login"
                                                  :method http/post
                                                  :req-params {:json-params {:name username
                                                                             :email email}}
                                                  :resp-event :event/login})
                    %))
                (#(if do-login?
                    (assoc-in % [:db :login :status] "inprogress")
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
                 (assoc % :user (-> res
                                    :body
                                    :user)))
               %)))})

(aux/init-page
 {:subs/login-username #(->> % :login :username)
  :subs/login-email    #(->> % :login :email)
  :subs/login-username-error #(->> % :login :username-error)
  :subs/login-email-error #(->> % :login :email-error)
  :subs/login-status #(->> % :login :status)}
 {:event/login-username set-username
  :event/login-email    set-email
  :event/login-link-start-click   link-start
  :event/login-register-click :login   ; TODO : register page
  :event/login login})

(defn- err-msg
  [msg]
  (if (str/blank? msg)
    nil
    [:p {:class "help is-danger"} msg]))

(defn- login-button
  [status]
  (let [class (case status
                "failed" "button is-primary is-danger"
                "inprogress" "button is-primary is-loading"
                "button is-primary")
        text (case status
               "failed" "FAILED"
               "inprogress" "Login..."
               "done" "SUCCESS"
               "LINK START")]
    [:button
     {:class class
      :on-click #(rf/dispatch [:event/login-link-start-click])}
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
         [:button.button "Register"]]]]]]]])





(defn page
  []
  (let [user @(rf/subscribe [:user])]
    (if (empty? user)
      (login-page)
      (rfe/push-state :user))))

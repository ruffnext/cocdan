(ns cocdan.page.login 
  (:require [cljs-http.client :as http]
            [clojure.core.async :refer [<! go timeout]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn page
  []
  (r/with-let [login-button-status (r/atom "is-link")
               username (r/atom "")

               on-login-button-click
               (fn [_]
                 (reset! login-button-status "is-loading")
                 (go (let [{:keys [status body]} (<! (http/post "/api/auth/login" {:edn-params {:username @username}}))]
                       (if (= status 200)
                         (do
                           (reset! login-button-status "is-success")
                           (rf/dispatch [:event/auth-login body])
                           (<! (timeout 2000))
                           (rf/dispatch [:common/navigate! :main {:nav "stages.list"} ])) 
                         (reset! login-button-status "is-danger")))))
               
               ]
    [:div.container.columns.is-centered.is-vcentered
     {:style {:min-width "100vw" :min-height "100vh"}}
     [:div.card
      {:style {:width "20em"
               :margin-right "auto" :margin-left "auto"}}
      [:div.card-content
       [:figure.image
        {:style {:margin-left "3em" :margin-right "3em"}}
        [:img {:src "/img/soslogo.jpg"}]]
       [:p.title.is-4 {:style {:text-align "center"}} "登录"]
       [:div.field
        [:label.label "用户名"]
        [:div.control
         [:input.input
          {:value @username
           :on-change #(reset! username (-> % .-target .-value))
           :placeholder "请输入用户名"}]]]
       [:div.field.is-grouped
        {:style {:padding-top "1em"}}
        [:div.control
         {:style {:margin-left "auto"}}
         [:button.button
          {:class @login-button-status
           :on-click on-login-button-click}
          "Login"]]
        [:div.control
         {:style {:margin-right "auto"}}
         [:button.button.is-primary "Register"]]]]]]))

(ns cocdan.routes.auth
  (:require [cats.monad.either :as either]
            [cocdan.services.auth :as auth]
            [cocdan.middleware :refer [wrap-restricted]]
            [cocdan.middleware.monad-api :refer [wrap-monad]]
            [cocdan.db.monad-db :refer [get-user-by-id
                                                get-user-by-username]]
            [ring.util.response :as response]))

(def routes
  ["/auth"
   {:swagger {:tags ["auth"]}}
   ["/login" {:post {:summary "登录"
                     :parameters {:body {:username string?}}
                     :handler (fn [{{{:keys [username]} :body} :parameters}]
                                (let [res (get-user-by-username username)]
                                  (either/branch
                                   res
                                   (fn [left] (response/bad-request {:error (str left)}))
                                   (fn [right] (-> (response/response right)
                                                   (assoc-in [:session :identity] (:id right)))))))}}]
   ["/logout" {:post {:summary "登出"
                      :parameters {}
                      :handler (wrap-restricted
                                (fn [_]
                                  (-> (response/response {:ok "ok"})
                                      (assoc-in [:session :identity] nil))))}}]
   ["/register" {:post {:summary "注册帐号"
                        :parameters {:body {:username string?
                                            :nickname string?}}
                        :handler (wrap-monad
                                  (fn [{{{:keys [username nickname]} :body} :parameters}]
                                    (auth/register! username nickname)))}}]
   ["/unregister" {:post {:summary "注销帐号"
                          :parameters {:body {:username string?}}
                          :handler (wrap-monad
                                    (fn [{{{:keys [username]} :body} :parameters}]
                                      (auth/unregister! username)))}}]
   ["" {:get {:summary "判断是否登录"
              :handler (wrap-restricted
                        (wrap-monad
                         (fn [{:keys [session]}]
                           (let [user-id (:identity session)]
                             (get-user-by-id user-id)))))}}]])
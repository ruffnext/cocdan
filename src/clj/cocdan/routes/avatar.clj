(ns cocdan.routes.avatar
  (:require [cocdan.middleware :refer [wrap-restricted]]
            [cocdan.middleware.monad-api :refer [wrap-monad]]
            [cocdan.schema :refer [Avatar]]
            [cocdan.services.avatar :refer :all]))

(def routes
  ["/avatar"
   {:swagger {:tags ["avatar"]}}
   ["/" {:post {:summary "创建角色"
                :parameters {:body Avatar}
                :handler (wrap-restricted
                          (wrap-monad
                           (fn [{{avatar :body} :parameters
                                 {user-id :identity} :session}]
                             (create-avatar! avatar user-id))))}}]
   ["/:id" {:get {:summary "根据 id 获得角色信息"
                  :parameters {:path {:id pos-int?}}
                  :responses {:200 {:body Avatar}}
                  :handler (wrap-restricted
                            (wrap-monad
                             (fn [{{{avatar-id :id} :path} :parameters}]
                               (get-avatar-by-id avatar-id))))}
            :post {:summary "更新角色的信息"
                   :parameters {:body Avatar :path {:id pos-int?}}
                   :handler (wrap-restricted
                             (wrap-monad
                              (fn [{{avatar :body {avatar-id :id} :path} :parameters
                                    {user-id :identity} :session}]
                                (update-avatar-by-id avatar-id avatar user-id))))}
            :delete {:summary "删除角色"
                     :parameters {:path {:id pos-int?}}
                     :handler (fn [_] ())}}]
   ["/list"
    ["/u:id" {:get {:summary "列出用户的所有角色信息"
                    :parameters {:path {:id pos-int?}}
                    :responses {:200 {:body [Avatar]}}
                    :handler (fn [_] ())}}]
    ["/s:id" {:get {:summary "列出舞台上所有角色的信息"
                    :parameters {:path {:id pos-int?}}
                    :responses {:200 {:body [Avatar]}}
                    :handler (fn [_] ())}}]]])
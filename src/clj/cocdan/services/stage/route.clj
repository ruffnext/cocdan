(ns cocdan.services.stage.route
  #_{:clj-kondo/ignore [:refer-all]}
  (:require [cocdan.middleware :refer [wrap-restricted]]
            [cocdan.middleware.monad-api :refer [wrap-monad]]
            [cocdan.schema :refer [Stage StageNew]]
            [cocdan.services.stage.core :refer :all]))

(def routes
  ["/stage"
   {:swagger {:tags ["stage"]}}
   ["" {:post {:summary "新建舞台"
               :parameters {:body StageNew}
               :handler (wrap-restricted
                         (wrap-monad
                          (fn [{{stage :body} :parameters
                                {user-id :identity} :session}]
                            (create-stage! stage user-id))))}}]
   ["/:id" {:get {:summary "获得舞台的信息"
                  :parameters {:path {:id int?}}
                  :responses {:200 {:body Stage}}
                  :handler (wrap-restricted
                            (wrap-monad
                             (fn [{{{id :id} :path} :parameters}]
                               (query-stage-by-id id))))}
            :post {:summary "更新舞台的信息"
                   :parameters {:body StageNew
                                :path {:id int?}}
                   :responses {:200 Stage}
                   :handler (wrap-restricted
                             (wrap-monad
                              (fn [{{stage :body
                                     {stage-id :id} :path} :parameters
                                    {user-id :identity} :session}]
                                (update-stage! stage-id stage user-id))))}
            :delete {:summary "删除舞台"
                     :parameters {:path {:id int?}}
                     :handler (wrap-restricted
                               (wrap-monad
                                (fn [{{{stage-id :id} :path} :parameters
                                      {user-id :identity} :session}]
                                  (delete-stage! stage-id user-id))))}}]
   ["/list/" {:get {:summary "获得已加入的舞台的信息"
                    :handler (wrap-restricted
                              (wrap-monad
                               (fn [{{user-id :identity} :session}]
                                 (get-stages-by-user-id user-id))))
                    :responses {:200 {:body [Stage]}}}}]])
(ns cocdan.services.journal.route 
  (:require [clojure.spec.alpha :as s]
            [cocdan.middleware :refer [wrap-restricted]]
            [cocdan.middleware.monad-api :refer [wrap-monad]]
            [cocdan.schema :refer [Speak Transact]]
            [cocdan.services.journal.core :as journal]))

(s/def ::begin int?)
(s/def ::limit int?)
(s/def ::with-context boolean?)

(def routes
  ["/journal/s:id"
   {:swagger {:tags ["journal"]}}
   ["" {:get {:summary "查询舞台的日志"
              :parameters {:path {:id int?}
                           :query (s/keys
                                   :opt-un [::begin ::limit ::with-context])}
              :handler (wrap-restricted
                        (wrap-monad
                         (fn [{{{stage-id :id} :path
                                {:keys [begin limit with-context] :or {limit 10 begin 0 with-context false}} :query} :parameters
                               {_user-id :identity} :session}]
                           (journal/list-transactions stage-id begin limit with-context))))}}]])

(def action-routes
  ["/action/a:id"
   {:swagger {:tags ["action"]}}
   ["/speak" {:post {:summary "执行舞台行动 - 说话"
                    :parameters {:body Speak
                                 :path {:id int?}}
                    :handler (wrap-restricted
                              (wrap-monad
                               (fn [{{speak-record :body
                                      {avatar-id :id} :path} :parameters
                                     {user-id :identity} :session}]
                                 (journal/m-speak avatar-id speak-record user-id))))}}]
   ["/transact" {:post {:summary "直接对舞台进行控制"
                        :parameters {:body Transact
                                     :path {:id int?}}
                        :handler (wrap-restricted
                                  (wrap-monad
                                   (fn [{{{avatar-id :id} :path
                                          {:keys [type props]} :body} :parameters
                                         {user-id :identity} :session}]
                                     (journal/service-transact avatar-id type props user-id))))}}]])


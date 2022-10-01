(ns cocdan.services.journal.route 
  (:require [clojure.spec.alpha :as s]
            [cocdan.middleware :refer [wrap-restricted]]
            [cocdan.middleware.monad-api :refer [wrap-monad]]
            [cocdan.schema :refer [Speak Transact]]
            [cocdan.services.journal.core :as journal]))

(s/def ::offset int?)
(s/def ::limit int?)
(s/def ::with-context boolean?)
(s/def ::desc boolean?)
(s/def ::begin int?)

(def routes
  ["/journal/s:id"
   {:swagger {:tags ["journal"]}}
   ["" {:get {:summary "查询舞台的日志"
              :parameters {:path {:id int?}
                           :query (s/keys
                                   :opt-un [::offset ::limit ::with-context ::desc ::begin])}
              :handler (wrap-restricted
                        (wrap-monad
                         (fn [{{{stage-id :id} :path
                                {:keys [offset limit with-context desc begin] 
                                 :or {begin 0 limit 10 offset 0 with-context false desc true}} :query} :parameters
                               {_user-id :identity} :session}]
                           (journal/list-transactions stage-id begin offset limit with-context (if desc :desc :asce)))))}}]])

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


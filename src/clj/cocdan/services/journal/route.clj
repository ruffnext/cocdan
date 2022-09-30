(ns cocdan.services.journal.route 
  (:require [clojure.spec.alpha :as s] 
            [cocdan.services.journal.core :as journal]
            [cocdan.middleware :refer [wrap-restricted]]
            [cocdan.middleware.monad-api :refer [wrap-monad]]))

(s/def ::begin int?)
(s/def ::limit int?)
(s/def ::recursed boolean?)

(def routes
  ["/journal"
   {:swagger {:tags ["journal"]}}
   ["/stage/:id" {:get {:summary "查询舞台的日志"
                        :parameters {:path {:id int?}
                                     :query (s/keys
                                             :opt-un [::begin ::limit ::recursed])}
                        :handler (wrap-restricted
                                  (wrap-monad
                                   (fn [{{{stage-id :id} :path
                                          {:keys [begin limit recursed] :or {limit 10 begin 0}} :query} :parameters
                                         {user-id :identity} :session}]
                                     (journal/list-transactions stage-id begin limit))))}}]
   ["/stage/:id/play" {:post {:summary "执行舞台行动"
                              :parameters {:body associative?
                                           :path {:id int?}
                                           :query {:type string?}}
                              :handler (wrap-restricted
                                        (wrap-monad
                                         (fn [{{op-record :body
                                                {stage-id :id} :path
                                                {transaction-type :type} :query} :parameters
                                               {user-id :identity} :session}]
                                           (journal/transact! stage-id transaction-type op-record user-id))))}}]])

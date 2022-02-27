(ns cocdan.core.log
  (:require
   [posh.reagent :as p]
   [datascript.core :as d]
   [cocdan.db :as gdb]
   [cocdan.auxiliary :refer [remove-db-perfix]]
   [cljs-http.client :as http]
   [clojure.core.async :refer [go <!]]
   [clojure.string :as str]
   [re-posh.core :as rp]))

(defn make-log
  [sender receiver stage-id content log-time log-type ctx-eid]
  {:receiver receiver
   :content content
   :stage stage-id
   :time log-time
   :sender sender
   :type log-type
   :ctx ctx-eid})

(defn posh-unread-message-count
  [ds avatar-id]
  (p/q '[:find (count ?mids) .
         :in $ ?avatar-id
         :where
         [?avatareid :avatar/id ?avatar-id]
         [?avatareid :avatar/latest-read-message-time ?latest]
         [?mids :log/receiver ?avatar-id]
         [?mids :log/time ?midstime]
         [(> ?midstime ?latest)]]
       ds
       avatar-id))

(defn posh-avatar-latest-message-time
  [ds avatar-id]
  (p/q '[:find (max ?time) .
         :in $ ?avatar-id
         :where
         [?e :log/receiver ?avatar-id]
         [?e :log/time ?time]]
       ds
       avatar-id))

(defn query-latest-messages-by-avatar-id
  [ds avatar-id limit]
  (->> (d/datoms @ds :avet :log/time)
       reverse
       (map :e)
       (reduce (fn [a x]
                 (let [message (d/pull @ds '[*] x)]
                   (if (= (:log/receiver message) avatar-id)
                     (conj a message)
                     a)))
               [])
       (take limit)
       reverse
       (map remove-db-perfix)))

(defn get-avatar-from-ctx
  [ctx-eid avatar-id]
  (let [ctx (d/pull @gdb/db '[*] ctx-eid)]
    (if (= avatar-id 0)
      {:name "system"
       :id 0
       :header ""
       :attributes {}}
      (first (filter #(= (:id %) avatar-id) (-> ctx :action/fact :avatars))))))

(defn query-log-by-receiver-and-time
  [ds receiver log-time]
  (d/q '[:find ?e .
         :in $ ?receiver ?time
         :where
         [?e :log/receiver ?receiver]
         [?e :log/time ?time]]
       ds
       receiver
       log-time))

(defn append-log
  [db log-item]
  (d/transact! db [(reduce (fn [a [k v]] (assoc a (keyword (str "log/" (name k))) v)) {} log-item)]))

(defn query-stage-action?
  [ds stage-id order]
  (let [res (d/q '[:find (pull ?e [*]) .
                   :in $ ?stage-id ?order
                   :where
                   [?e :action/stage ?stage-id]
                   [?e :action/order ?order]]
                 ds
                 stage-id
                 order)]
    (if (nil? res)
      nil
      (assoc (remove-db-perfix res) :db/id (:db/id res)))))

(defn query-log-eid
  [ds receiver time]
  (d/q '[:find ?e .
         :in $ ?receiver ?time
         :where
         [?e :log/receiver ?receiver]
         [?e :log/time ?time]]
       ds
       receiver
       time))

(defn append-action!
  [db actions]
  (if (vector? actions)
    (let [res (->> (map (fn [{stage-id :stage order :order :as action}]
                          (let [res (query-stage-action? @db stage-id order)
                                new-action (reduce (fn [a [k v]] (assoc a (keyword (str "action/" (name k))) v)) {} action)]
                            (if res
                              (assoc new-action :db/id (:db/id res))
                              new-action)))
                        actions)
                   (filter #(not (nil? %)))
                   vec)]
      (when (seq res)
        (d/transact! db res)))
    (append-action! db [actions])))

(defn query-action-ctx?
  [ds stage-id current-order]
  (let [res (->> (d/q '[:find [(pull ?eid [*]) ...]
                        :in $ ?stage-id ?current-order
                        :where
                        [?eid :action/stage ?stage-id]
                        [?eid :action/type ?action-type]
                        [?eid :action/order ?order]
                        [(<= ?order ?current-order)]]
                      ds
                      stage-id
                      current-order)
                 (sort-by :action/order)
                 reverse)
        last-ctx' (when (= (:action/order (first res)) current-order)
                    (reduce (fn [a x]
                              (if (and
                                   (>= (:action/order a) (- 1 (:action/order x)))
                                   (not= (:action/type x) "snapshot"))
                                x
                                (reduced x)))
                            res))]
    (if (= (:action/type last-ctx') "snapshot")
      (assoc (remove-db-perfix last-ctx') :eid (:db/id last-ctx'))
      nil)))

(defn posh-stage-latest-ctx-eid
  [db stage-id]
  (let [ctx-eid (->> @(p/q '[:find ?order ?ctx-eid
                             :in $ ?stage-id
                             :where
                             [?ctx-eid :action/type "snapshot"]
                             [?ctx-eid :action/stage ?stage-id]
                             [?ctx-eid :action/order ?order]]
                           db
                           stage-id)
                     (sort-by first)
                     reverse
                     first
                     second)]
    ctx-eid))

(defn- query-pull-stage-latest-ctx
  [ds stage-id]
  (let [ctx-eid (->> (d/q '[:find ?order ?ctx-eid
                            :in $ ?stage-id
                            :where
                            [?ctx-eid :action/type "snapshot"]
                            [?ctx-eid :action/stage ?stage-id]
                            [?ctx-eid :action/order ?order]]
                          ds
                          stage-id)
                     (sort-by first)
                     reverse
                     first
                     second)
        res (d/pull ds '[*] ctx-eid)]
    (-> (remove-db-perfix res)
        (assoc :db/id (:db/id res)))))

(comment
  (posh-stage-latest-ctx-eid gdb/db 2)
  )

(defn query-stage-latest-action-order
  [ds stage-id]
  (or (d/q '[:find (max ?order) .
             :in $ ?stage-id
             :where
             [?e :action/stage ?stage-id]
             [?e :action/order ?order]]
           ds
           stage-id)
      0))

(defn query-stage-action-by-order
  [ds stage-id order]
  (-> (d/q '[:find (pull ?e [*]) .
             :in $ ?stage-id ?order
             :where
             [?e :action/stage ?stage-id]
             [?e :action/order ?order]]
           ds
           stage-id
           order)
      remove-db-perfix))

(defn query-following-actions
  [ds stage-id current-order]
  (let [res (->> (d/q '[:find [(pull ?eid [*]) ...]
                        :in $ ?stage-id ?current-order
                        :where
                        [?eid :action/stage ?stage-id]
                        [?eid :action/type ?action-type]
                        [?eid :action/order ?order]
                        [(>= ?order ?current-order)]]
                      ds
                      stage-id
                      current-order)
                 (sort-by :action/order))
        following-actions (when (and 
                                 (= (:action/order (first res)) current-order)
                                 (= (:action/order (second res)) (+ 1 current-order)))
                            (reduce (fn [a x]
                                      (if (and
                                           (not= (:action/type x) "snapshot")
                                           (<= (+ (:action/order (last a)) 1) (:action/order x)))
                                        (conj a x)
                                        (reduced a)))
                                    []
                                    (rest res)))]
    (if (seq following-actions)
      following-actions
      nil)))

(defn- build-avatar-stage-ctx
  [{{avatars :avatars
    stage :stage} :fact} {{substage :substage} :attributes :as _avatar}]
  {:avatars {:same-stage (vec (filter #(= (-> % :attributes :substage) substage) avatars))
             :next-stage (let [next-substages (-> (keyword substage (-> stage :attributes :substages)) :coc :连通区域 set)]
                           (vec (filter #(contains? next-substages (-> % :attributes :substage)) avatars)))}})

(defn- handle-speak
  [{{avatars :avatars} :fact eid :eid :as ctx} {{sender :avatar msg :msg :as _fact} :fact action-time :time stage-id :stage action-type :type}]
  (let [speak-avatar (first (filter #(= (:id %) sender) avatars))
        speaker-env (build-avatar-stage-ctx ctx speak-avatar)]
    (vec (for [{receiver :id} (-> speaker-env :avatars :same-stage)]
           (make-log sender receiver stage-id msg action-time action-type eid)))))

(defn- handle-use
  [{{avatars :avatars} :fact eid :eid :as ctx} {{sender :avatar item-used :use action :msg :as _fact} :fact action-time :time stage-id :stage action-type :type}]
  (let [actor (first (filter #(= (:id %) sender) avatars))
        actor-env (build-avatar-stage-ctx ctx actor)]
    (vec (for [{receiver :id} (-> actor-env :avatars :same-stage)]
           (make-log sender receiver stage-id (str "使用" (str/join "," item-used) "，" action) action-time action-type eid)))))

(defn- handle-system-msg
  [{{{stage-admin :owned_by} :stage avatars :avatars} :fact eid :eid} {{sender :avatar msg :msg receiver-id :receiver substage :substage :as fact} :fact action-time :time stage-id :stage}]
  (when (not= sender 0)
    (js/console.log "this system msg is not send from system (id = 0)")
    (js/console.log fact))
  (let [receivers (-> (if (nil? receiver-id)
                       (map :id (filter #(= substage (-> % :attributes :substage)) avatars))
                       (list receiver-id))
                     (conj stage-admin)
                     set)]
    (vec (for [receiver receivers]
           (make-log 0 receiver stage-id msg action-time "system-msg" eid)))))

(defn- parse-action
  [ctx action]
  (cond
    (vector? action) (flatten (map #(parse-action ctx %) action))
    :else (let [{action-type :type} action]
            (cond
              (str/starts-with? action-type "speak") (handle-speak ctx action)
              (str/starts-with? action-type "use") (handle-use ctx action)
              (= action-type "system-msg") (handle-system-msg ctx action)
              :else (do
                      (js/console.log (str "unreadered action " action-type))
                      (js/console.log action)
                      [])))))

(defn- check-missing-history-actions!
  [ds stage-id current-order]
  (let [res (->> (d/q '[:find (?order ...)
                        :in $ ?stage-id ?current-order
                        :where
                        [?eid :action/stage ?stage-id]
                        [?eid :action/type ?action-type]
                        [?eid :action/order ?order]
                        [(< ?order ?current-order)]]
                      ds
                      stage-id
                      current-order)
                 set)
        missing-actions (->> (reduce (fn [a x]
                                       (if (contains? res x) a (conj a x)))
                                     [] (reverse (range 1 current-order)))
                             (take 20))]
    (when (seq missing-actions)
      (go
        (let [{body :body status :status} (<! (http/get (str "/api/stage/s" stage-id "/history-actions") {:query-params {:orders (str/join "," missing-actions)}}))]
          (when (and (= status 200) (seq body))
            (append-action! gdb/db body)))))))

(defn- rebuild-action-from-tx-data
  [tx-data]
  (let [eids (reduce (fn [a [eid attr & _r]]
                       (if (= attr :action/order) (conj a eid) a))
                     [] tx-data)
        res (reduce (fn [a [eid attr val & _r]]
                      (if (contains? (set eids) eid)
                        (assoc-in a [(.indexOf eids eid) attr] val)
                        a))
                    (vec (map (fn [x] {:eid x}) eids))
                    tx-data)]
    (map remove-db-perfix res)))

(defn action-to-log!
  [report transact-map]
  (let [logs (cond
               (= (:type transact-map) "snapshot") ;; re-render following actions
               (when-let [actions (query-following-actions (:db-after report) (:stage transact-map) (:order transact-map))]
                 (parse-action transact-map (vec (map remove-db-perfix actions)))
                 (when-let [{{stage :stage avatars :avatars} :fact} (query-pull-stage-latest-ctx (:db-after report) (:stage transact-map))]
                   (rp/dispatch [:rpevent/upsert :stage stage])
                   (rp/dispatch [:rpevent/upsert :avatar avatars])))

               (:fact transact-map) ;; render this action
               (when-let [ctx (query-action-ctx? (:db-after report) (:stage transact-map) (:order transact-map))]
                 (parse-action ctx [(remove-db-perfix transact-map)]))

               :else [])]
    (doseq [{receiver :receiver
             log-time :time :as log-item} logs]
      (when (and receiver log-time (not (query-log-by-receiver-and-time @gdb/db receiver log-time)))
        (append-log gdb/db log-item)))))

(defn register-action-to-log-listener!
  [db]
  (d/listen! db :action-to-log (fn [report]
                                 (let [tx-data (-> report :tx-data)
                                       transact-maps (rebuild-action-from-tx-data tx-data)
                                       stage-ids (set (map :stage transact-maps))]
                                   (doseq [transact-map transact-maps]
                                     (action-to-log! report transact-map))
                                   (doseq [stage-id stage-ids]
                                     (check-missing-history-actions! (:db-after report) stage-id (query-stage-latest-action-order (:db-after report) stage-id)))))))

(comment
  [(reduce (fn [a [k v]] (assoc a (keyword (str "action/" (name k))) v)) {} {:order 1 :time 2})]
  (get-avatar-from-ctx 17 2)
  (d/pull @gdb/db '[*] 17)
  (query-latest-messages-by-avatar-id gdb/db 2 10)
  (rp/dispatch [:rpevent/upsert :avatar [{:id 7 :attributes {:foo "barr"}}]])
  (d/pull @gdb/db '[*] [:avatar/id 7])
  )
(ns cocdan.db
  (:require
   [datascript.core :as d]
   [re-posh.core :as rp]
   [clojure.string :as str]
   [posh.reagent :as p]))

(def schema
  {:stage/id {:db/unique :db.unique/identity}
   :avatar/id {:db/unique :db.unique/identity}
   :avatar/messages {:db/cardinality :db.cardinality/many}
   ;:message/time {:db/unique :db.unique/identity}
   })

(defonce conn (d/create-conn schema))
(rp/connect! conn)

(defn- handle-key
  [base k]
  (keyword (str (name base) "/" (name k))))

(defn- handle-keys
  [base attrs]
  (reduce (fn [a [k v]]
            (assoc a (handle-key base k) v)) {} attrs))

(defn- remove-perfix
  [k]
  (keyword (first (str/split (name k) "/" 1))))

(defn remove-db-perfix
  [vals]
  (cond
    (or (vector? vals)
        (list? vals)) (map remove-db-perfix vals)
    :else (reduce (fn [a [k v]]
                    (assoc a (remove-perfix k) v)) {} (dissoc vals :db/id))))



(rp/reg-pull-sub
 :rpsub/entity
 '[*])

(rp/reg-event-ds
 :rpevent/upsert
 (fn [_ds [_ col-key attrs]]
   (cond
     (or (list? attrs)
         (vector? attrs)
         (seq? attrs)) (doall (for [attr attrs]
                                (handle-keys col-key attr)))
     (map? attrs) [(handle-keys col-key attrs)]
     :else [])))

(defn query-stage-avatar-ids
  [ds stage-id]
  (reduce into []
          (d/q '[:find ?id
                 :in $ ?a1
                 :where [?id :avatar/on_stage ?a1]]
               ds stage-id)))

(defn- query-stage-ids
  [ds stage-id]
  (first (reduce into []
                 (d/q '[:find ?id
                        :in $ ?stage-id
                        :where [?id :stage/id ?stage-id]]
                      ds stage-id))))

(defn query-stage-substage-ids
  [ds stage-id]
  (->> (d/entid ds [:stage/id stage-id])
       (d/pull ds '[*])))

(comment
  (query-stage-substage-ids @conn 1))

(defn query-substage-message-ids
  [ds stage-id substage-id]
  (reduce into []
          (d/q '[:find ?id
                 :in $ ?stage-id ?substage-id
                 :where
                 [?id :message/mainstage ?stage-id]
                 [?id :message/substage ?substage-id]]
               ds stage-id substage-id)))

(defn- query-message-ids
  [ds]
  (reduce into []
          (d/q '[:find ?id
                 :where [?id :message/time]]
               ds)))

(defn- query-my-info-ids
  [ds]
  (first (reduce into []
                 (d/q '[:find ?id
                        :where [?id :my-info/id]]
                      ds))))

(defn query-my-avatars-ids
  [ds]
  (reduce into []
          (d/q '[:find ?id
                 :in $ ?user-id
                 :where [?id :avatar/controlled_by ?user-id]]
               ds
               (query-my-info-ids ds))))

(defn- query-avatar-ids
  [ds avatar-id]
  (first (reduce into []
                 (d/q '[:find ?id
                        :in $ ?avatar-id
                        :where [?id :avatar/id ?avatar-id]]
                      ds avatar-id))))

(defn posh-current-use-avatar-id
  [ds stage-id]
  (->> @(p/q '[:find ?v
               :in $ ?stage-id
               :where
               [?eid :stage/id ?stage-id]
               [?eid :stage/current-use-avatar ?v]
               [?aid :avatar/id ?v]
               [?aid :avatar/name ?n]]
             ds
             stage-id)
       (reduce into [])
       first))

(defn posh-unread-message-count
  [ds avatar-id]
  (->> @(p/q '[:find ?mids
               :in $ ?avatar-id
               :where
               [?avatareid :avatar/id ?avatar-id]
               [?avatareid :avatar/latest-read-message-time ?latest]
               [?mids :message/receiver ?avatar-id]
               [?mids :message/time ?midstime]
               [(> ?midstime ?latest)]]
             ds
             avatar-id)))

(comment
  (posh-unread-message-count conn 2)
  )

(defn posh-current-use-avatar
  [ds stage-id]
  (let [avatar-id (posh-current-use-avatar-id ds stage-id)
        avatar-eid (->> @(p/q '[:find ?aid
                                :in $ ?avatar-id
                                :where [?aid :avatar/id ?avatar-id]]
                              ds
                              avatar-id)
                        (reduce into [])
                        first)]
    (-> @(p/pull ds '[*] avatar-eid)
        remove-db-perfix)))

(defn posh-avatar-by-stage-id
  [ds stage-id]
  (let [avatars-ids (reduce into []
                            @(p/q '[:find ?id
                                    :in $ ?stage-id
                                    :where [?id :avatar/on_stage ?stage-id]]
                                  ds
                                  stage-id))
        avatars (->> @(p/pull-many ds '[*] avatars-ids)
                     remove-db-perfix)]
    avatars))

(defn posh-stage-by-id
  [ds stage-id]
  (let [stage-eid (->> @(p/q '[:find ?sid
                               :in $ ?stage-id
                               :where [?sid :stage/id ?stage-id]]
                             ds
                             stage-id)
                       (reduce into [])
                       first)]
    (-> @(p/pull conn '[*] stage-eid)
        remove-db-perfix)))

(defn query-my-avatars
  [ds]
  (let [my-avatars-id (query-my-avatars-ids ds)]
    (for [avatar (d/pull-many ds '[*] my-avatars-id)]
      (remove-db-perfix avatar))))

(defn posh-my-info
  [ds]
  (let [my-eid
        (->>
         @(p/q '[:find ?e
                 :where [?e :my-info/id ?a]]
               ds)
         (reduce into [])
         first)]
    (->> @(p/pull ds '[*] my-eid)
         remove-db-perfix)))

(defn posh-my-avatars
  [ds]
  (let [my-user-id (:id (posh-my-info ds))
        my-avatars-id (->>
                       @(p/q '[:find ?avatar-id
                               :in $ ?my-user-id
                               :where [?avatar-id :avatar/controlled_by ?my-user-id]]
                             ds my-user-id)
                       (reduce into []))
        my-avatars (->>
                    @(p/pull-many ds '[*] my-avatars-id))]
    (vec (map remove-db-perfix my-avatars))))

(defn posh-i-have-control?
  [ds stage-id]
  (let [{owned_by :owned_by} (posh-stage-by-id ds stage-id)
        controller (->> (posh-my-avatars ds)
                        (map remove-db-perfix)
                        (filter #(= (:id %) owned_by)))]
    (if (seq controller)
      (first controller)
      nil)))

(defn query-avatars-by-stage-id ; not use posh, be careful!
  [ds stage-id]
  (let [avatars (->> (d/q '[:find ?e
                            :in $ ?stage-id
                            :where [?e :avatar/on_stage ?stage-id]]
                          ds
                          stage-id)
                     (reduce into []))]
    (-> (d/pull-many ds '[*] avatars)
        remove-db-perfix)))

(defn query-avatar-by-id
  [ds avatar-id]
  (let [aid (->> (d/q '[:find ?e
                        :in $ ?avatar-id
                        :where [?e :avatar/id ?avatar-id]]
                      ds
                      avatar-id)
                 (reduce into [])
                 first)]
    (-> (d/pull ds '[*] aid)
        remove-db-perfix)))

(rp/reg-sub
 :rpsub/stage-avatars
 (fn [ds [_dirven-by stage-id]]
   {:type :pull-many
    :pattern '[*]
    :ids (query-stage-avatar-ids ds stage-id)}))

(rp/reg-sub
 :rpsub/stage
 (fn [ds [_driven-by stage-id]]
   {:type :pull
    :pattern '[*]
    :id (query-stage-ids ds stage-id)}))

(rp/reg-sub
 :rpsub/stage-substages
 (fn [ds [_driven-by stage-id]]
   {:type :pull-many
    :pattern '[*]
    :ids (query-stage-substage-ids ds stage-id)}))

(rp/reg-sub
 :rpsub/stage-substage-messages
 (fn [ds [_driven-by stage-id substage-id]]
   {:type :pull-many
    :pattern '[*]
    :ids (query-substage-message-ids ds stage-id substage-id)}))

(rp/reg-sub
 :rpsub/my-info
 (fn [ds [_driven-by]]
   {:type :pull
    :pattern '[*]
    :id (query-my-info-ids ds)}))

(rp/reg-sub
 :rpsub/my-avatars
 (fn [ds [_driven-by]]
   {:type :pull-many
    :pattern '[*]
    :ids (query-my-avatars-ids ds)}))

(rp/reg-sub
 :rpsub/messages
 (fn [ds [_driven-by]]
   (js/console.log (query-message-ids ds))
   {:type :pull-many
    :pattern '[*]
    :ids (query-message-ids ds)}))

(rp/reg-sub
 :rpsub/avatar
 (fn [ds [_driven-by avatar-id]]
   {:type :pull
    :pattern '[*]
    :id (query-avatar-ids ds avatar-id)}))

(comment
  (query-stage-avatar-ids @conn 1)
  (d/pull @conn '[*] 8)
  @(rp/subscribe [:rpsub/stage-avatars 1])
  @(rp/subscribe [:rpsub/stage-substages 1])
  @(rp/subscribe [:rpsub/stage 2])
  (posh-stage-by-id conn 2)
  @(p/q '[:find ?sid
          :in $ ?stage-id
          :where [?sid :stage/id ?stage-id]]
        conn
        2)
  @(rp/subscribe [:rpsub/my-info])
  @(rp/subscribe [:rpsub/my-avatars])
  @(rp/subscribe [:rpsub/stage-substage-messages 1 0])
  @(rp/subscribe [:rpsub/messages])
  @(rp/subscribe [:rpsub/avatar 2])
  (rp/dispatch [:rpevent/upsert :message {:message/time 9}])
  (rp/dispatch [:rpevent/upsert :someother {:foo "bar"}])
  (d/q '[:find ?id
         :where [?id :message/time]]
       @conn)
  @(p/q '[:find ?id
          :where [?id :message/time]]
        conn)
  (d/q '[:find ?e
         :where [?e :avatar/id]] @conn)
  (d/pull @conn '[*]  (d/entid @conn [:avatar/id 1]))
  (->> (d/q '[:find ?eid
              :where
              [?eid :message/receiver 2]]
            @conn)
       (take 10))
  (d/transact conn [{:avatar/id 2
                     :avatar/messages [{:db/id -1 :message/msg "Hello" :message/time 23}]}])
  (d/pull @conn '[*] 2)

  (d/q '[:find ?ms
         :where
         [?a :avatar/id 2]
         [?a :avatar/messages ?ms]]
       @conn)
  (d/entity @conn 18)
  (d/pull @conn '[*] 18)
  )



(def defaultDB
  {:stage-simple {:stage-simple-edit-modal-active false
                  :stage-simple-edit-modal-submit-status "is-primary"
                  :stage-simple-edit-modal {}}
   :login {:username ""
           :email ""
           :username-error nil
           :email-error nil
           :status "default"
           :register-status "default"}
   :avatars []; all avatars
   :stages [] ; all stages
   :user {}   ; you
   :users []  ; all users
   :chat []})

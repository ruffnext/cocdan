(ns cocdan.fragment.chat-log.core
  (:require [cocdan.aux :refer [remove-db-prefix]]
            [cocdan.data.mixin.territorial :refer [get-substage-id
                                                   ITerritorialMixIn]]
            [cocdan.data.mixin.visualization :refer [IChatLogVisualization
                                                     to-chat-log]]
            [cocdan.database.ctx-db.core :as ctx-db]
            [cocdan.fragment.chat-log.dice]
            [cocdan.fragment.chat-log.narration]
            [cocdan.fragment.chat-log.patch]
            [cocdan.fragment.chat-log.speak]
            [datascript.core :as d]
            [re-frame.core :as rf]))

(defonce chat-log-ui-cache-lazy (atom {}))

;; 一般来说不需要频繁地刷新缓存
(rf/reg-event-fx
 :chat-log/clear-cache!
 (fn [& _]
   (reset! chat-log-ui-cache-lazy {}) {}))

(defn- reset-chat-log-ui-cache-lazy
  "更新缓存"
  [stage-id avatar-id logs]
  (swap! chat-log-ui-cache-lazy #(assoc-in % [:logs (keyword (str stage-id)) (keyword (str avatar-id))] logs)))

(defn- transaction-to-log-hiccup
  "返回一个 [tid hiccup] / false 的结构"
  [stage-ds avatar-id {:keys [id ctx_id props] :as transaction}]
  (let [context (ctx-db/query-ds-ctx-by-id stage-ds ctx_id)
        avatar (get-in context [:context/props :avatars (keyword (str avatar-id))])]
    (js/console.log "to hiccup?")
    (cond
      (or (empty? context) (empty? avatar)) false
      (not (satisfies? IChatLogVisualization props)) false
      (and (satisfies? ITerritorialMixIn props) 
           (and
            (not= avatar-id 0) ;; 如果是 kp 的话应该能看到所有舞台的信息
            (not= (get-substage-id props) (get-substage-id avatar)))) false
      :else [id (with-meta (to-chat-log props
                                        context
                                        transaction
                                        avatar-id) {:key (str avatar-id "-" id)})])))

(defn- get-cache-range
  [stage-id avatar-id]
  (get-in @chat-log-ui-cache-lazy [:range (keyword (str stage-id)) (keyword (str avatar-id))]))

(defn- update-cache-range
  [stage-id avatar-id min-tid max-tid]
  (swap! chat-log-ui-cache-lazy
         (fn [y]
           (update-in y [:range (keyword (str stage-id)) (keyword (str avatar-id))]
                      (fn [{current-min :min-tid current-max :max-tid :as x}]
                        (cond
                          (nil? x) {:min-tid min-tid :max-tid max-tid}
                          (and (< min-tid current-min) (> max-tid current-max)) {:min-tid min-tid :max-tid max-tid}
                          (< min-tid current-min) (assoc x :min-tid min-tid)
                          (> max-tid current-max) (assoc x :max-tid max-tid)
                          :else x))))))

(defn- get-cached-logs
  [stage-id avatar-id]
  (get-in @chat-log-ui-cache-lazy [:logs (keyword (str stage-id)) (keyword (str avatar-id))]))

(defn- lazy-refresh-and-lazy-get-log-lazy-cache
  [stage-id avatar-id limit]
  (let [stage-ds @(ctx-db/query-stage-db stage-id)
        transactions-reverse (->> (reverse (d/datoms stage-ds :avet :transaction/id))
                                  (map #(remove-db-prefix (d/pull stage-ds '[*] (:e %))))
                                  (filter :ack)
                                  (#(if limit (take limit %) %)))
        transactions (reverse transactions-reverse)
        {:keys [min-tid max-tid]} (get-cache-range stage-id avatar-id)
        cached-logs (get-cached-logs stage-id avatar-id)

        cache-fn #(->> (map (partial transaction-to-log-hiccup stage-ds avatar-id) %) 
                       (filter identity))

        last-tid (:id (last transactions))
        first-tid (:id (first transactions))

        head-datoms (if max-tid (take (- last-tid max-tid) transactions-reverse) transactions-reverse)
        tail-datoms (if min-tid (take (- min-tid first-tid) transactions) [])

        cache-append-head (cache-fn head-datoms)
        cache-append-tail (cache-fn tail-datoms)
        res (concat cache-append-head cached-logs cache-append-tail)] 
    (update-cache-range stage-id avatar-id first-tid last-tid)
    (reset-chat-log-ui-cache-lazy stage-id avatar-id res)
    res))

(defn get-no-ack-log
  [stage-id avatar-id]
  (let [stage-ds @(ctx-db/query-stage-db stage-id)
        results (->> (reverse (d/datoms stage-ds :avet :transaction/id))
                     (map #(remove-db-prefix (d/pull stage-ds '[*] (:e %))))
                     (take-while #(not (:ack %)))
                     (map (partial transaction-to-log-hiccup stage-ds avatar-id)))]
    results))

(defn chat-log
  [{:keys [stage-id observer limit] :or {limit 10}}]
  (let [_refresh @(rf/subscribe [:partial-refresh/listen :chat-log])
        chat-log-verified (lazy-refresh-and-lazy-get-log-lazy-cache stage-id observer limit)
        chat-log-unverified (get-no-ack-log stage-id observer)
        logs (->> (concat chat-log-verified chat-log-unverified)
                  (sort-by first) 
                  (map second)
                  (reverse))]
    [:div.chat-log
     [:p.chat-anchor "-- end --"]
     (if observer
       logs
       [:p "您没有角色在这个舞台上"])]))

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

(defonce chat-log-by-avatar (atom {}))
(defonce chat-log-all-viewed-by-kp (atom {}))

;; 一般来说不需要频繁地刷新缓存
(rf/reg-event-fx
 :chat-log/clear-cache!
 (fn [& _]
   (reset! chat-log-by-avatar {})
   (reset! chat-log-all-viewed-by-kp {}) {}))

(defn- reset-chat-log-ui-cache-lazy
  "更新缓存"
  [stage-id avatar-id logs]
  (swap! chat-log-by-avatar #(assoc-in % [:logs (keyword (str stage-id)) (keyword (str avatar-id))] logs)))

(defn- visualization-log-by-avatar
  "返回一个 [tid hiccup] / false 的结构
   历史记录的渲染是追踪 avatar 的"
  [stage-ds avatar-id {:keys [id ctx_id props] :as transaction}]
  (let [context (ctx-db/query-ds-ctx-by-id stage-ds ctx_id)
        avatar (get-in context [:context/props :avatars (keyword (str avatar-id))])
        substage-id (if (satisfies? ITerritorialMixIn props) (get-substage-id props) nil)] 
    (cond
      (or (empty? context) (empty? avatar)) false
      (not (satisfies? IChatLogVisualization props)) false
      (and (satisfies? ITerritorialMixIn props)
           (not= avatar-id 0) ;; kp 需要渲染所有舞台上的日志
           (not= (get-substage-id props) (get-substage-id avatar))) false
      :else [id substage-id (with-meta (to-chat-log props
                                                    context
                                                    transaction
                                                    avatar-id) {:key (str avatar-id "-" id)})])))

(defn- update-logs-cache-by-avatar!
  [stage-id avatar-id first-tid last-tid res]
  (swap! chat-log-by-avatar
         (fn [y]
           (update-in y [:range (keyword (str stage-id)) (keyword (str avatar-id))]
                      (fn [{current-min :min-tid current-max :max-tid :as x}]
                        (cond
                          (nil? x) {:min-tid first-tid :max-tid last-tid}
                          (and (< first-tid current-min) (> last-tid current-max)) {:min-tid first-tid :max-tid last-tid}
                          (< first-tid current-min) (assoc x :min-tid first-tid)
                          (> last-tid current-max) (assoc x :max-tid last-tid)
                          :else x)))))
  (reset-chat-log-ui-cache-lazy stage-id avatar-id res))

(defn- get-lazy-chat-log-by-avatar
  [stage-id avatar-id limit]
  (let [stage-ds @(ctx-db/query-stage-db stage-id)
        transactions-reverse (->> (reverse (d/datoms stage-ds :avet :transaction/id))
                                  (map #(remove-db-prefix (d/pull stage-ds '[*] (:e %))))
                                  (filter :ack)
                                  (#(if limit (take limit %) %)))
        transactions (reverse transactions-reverse)

        ;; 获得历史消息缓存中最大和最小的 tid
        {:keys [min-tid max-tid]}
        (get-in @chat-log-by-avatar [:range (keyword (str stage-id)) (keyword (str avatar-id))])

        ;; 获得历史消息缓存的 lazy seq
        ;; 因为是 lazy seq，因此并不能计算其最大的和最小的 tid
        ;; 因为这意味着要把整个 seq 都渲染完后才能知道，就没有 lazy 的意义了
        ;; 因此需要保存最大和最小的 tid
        cached-logs 
        (get-in @chat-log-by-avatar [:logs (keyword (str stage-id)) (keyword (str avatar-id))])

        translator-fn (partial visualization-log-by-avatar stage-ds avatar-id)

        cache-fn #(->> (map translator-fn %)
                       (filter identity))

        last-tid (:id (last transactions))
        first-tid (:id (first transactions))

        head-datoms (if max-tid (take (- last-tid max-tid) transactions-reverse) transactions-reverse)
        tail-datoms (if min-tid (take (- min-tid first-tid) transactions) [])

        cache-append-head (cache-fn head-datoms)
        cache-append-tail (cache-fn tail-datoms)
        res (concat cache-append-head cached-logs cache-append-tail)]
    (update-logs-cache-by-avatar! stage-id avatar-id first-tid last-tid res)
    res))

(defn get-no-ack-log
  [stage-id avatar-id]
  (let [stage-ds @(ctx-db/query-stage-db stage-id)
        results (->> (reverse (d/datoms stage-ds :avet :transaction/id))
                     (map #(remove-db-prefix (d/pull stage-ds '[*] (:e %))))
                     (take-while #(not (:ack %)))
                     (map (partial visualization-log-by-avatar stage-ds avatar-id)))]
    results))

(defn- query-chat-log-by-avatar
  [stage-id avatar-id limit]
  (let [chat-log-verified (get-lazy-chat-log-by-avatar stage-id avatar-id limit)
        chat-log-unverified (get-no-ack-log stage-id avatar-id)]
    (->> (concat chat-log-verified chat-log-unverified)
         (sort-by first)
         (map (fn [[_ _ c]] c))
         (reverse))))

(defn- query-chat-log-by-substage
  "能够以 substage 视角看历史消息的只有 KP"
  [stage-id substage-id limit]
  (let [chat-log-verified (get-lazy-chat-log-by-avatar stage-id 0 limit)
        chat-log-unverified (get-no-ack-log stage-id 0)] 
    (->> (concat chat-log-verified chat-log-unverified)
         (filter (fn [[_id substage _]] (or (nil? substage) (= substage-id substage))))
         (sort-by first)
         (map (fn [[_ _ c]] c))
         (reverse))))

(defn chat-log
  [{:keys [stage-id substage-id observer limit] :or {limit 10}}]
  (let [_refresh @(rf/subscribe [:partial-refresh/listen :chat-log])
        logs (if (= 0 observer)
               (query-chat-log-by-substage stage-id substage-id limit)
               (query-chat-log-by-avatar stage-id observer limit))]
    [:div.chat-log
     [:p.chat-anchor "-- end --"]
     (if observer
       logs
       [:p "您没有角色在这个舞台上"])]))

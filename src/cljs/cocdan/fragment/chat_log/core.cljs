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
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as rd]
            [cats.monad.either :as either]))

;; 消息 HTML 的缓存
;; {:range {:stage-id {:avatar-id {:max-tid xxx :min-tid xxx}}}
;;  :logs {:stage-id {:avatar-id lazy-seq}}}
(defonce chat-log-by-avatar (atom {}))

;; 一般来说不需要频繁地刷新缓存
(rf/reg-event-fx
 :chat-log/clear-cache!
 (fn [& _]
   (reset! chat-log-by-avatar {})
   {}))

(defn- reset-chat-log-ui-cache-lazy
  "更新缓存"
  [stage-id avatar-id logs]
  (swap! chat-log-by-avatar #(assoc-in % [:logs (keyword (str stage-id)) (keyword (str avatar-id))] logs)))

(defn- visualization-log-by-avatar
  "返回一个 [tid substage-id hiccup] / false 的结构
   历史记录的渲染是追踪 avatar 的"
  [stage-ds avatar-id {:keys [id ctx_id payload] :as transaction}] 
  (let [context (ctx-db/query-ds-ctx-by-id stage-ds ctx_id)
        avatar (get-in context [:payload :avatars (keyword (str avatar-id))])
        substage-id (if (satisfies? ITerritorialMixIn payload) (get-substage-id payload) nil)] 
    
    (cond
      
      (or (empty? context) (empty? avatar)) false
      
      (not (satisfies? IChatLogVisualization payload)) false
      
      (and (satisfies? ITerritorialMixIn payload)
           (not= avatar-id 0) ;; kp 需要渲染所有舞台上的日志
           (not= (get-substage-id payload) (get-substage-id avatar))) false
      
      :else [id substage-id (with-meta (to-chat-log payload
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
  (let [stage-ds @(:history-db @(ctx-db/query-stage-db stage-id))

        ;; 获得历史消息缓存中最大和最小的 tid
        {:keys [min-tid max-tid]}
        (get-in @chat-log-by-avatar [:range (keyword (str stage-id)) (keyword (str avatar-id))])

        ;; d/datoms, map, filter 都是 lazy 的，因此实际上并没有将所有的记录都取出
        transactions (->> (d/datoms stage-ds :avet :transaction/id)
                          (map #(d/pull stage-ds '[*] (:e %)))
                          (filter :ack))

        transaction-head (if min-tid
                           (take-while (fn [{tid :id}] (< tid min-tid)) transactions)
                           transactions)

        transaction-tail (if max-tid
                           (take-while (fn [{tid :id}]  (< max-tid tid)) (reverse transactions))
                           [])

        this-first-tid (-> transactions first :id)
        this-last-tid (-> transactions last :id)

        ;; 获得历史消息缓存的 lazy seq
        ;; 因为是 lazy seq，因此并不能计算其最大的和最小的 tid
        ;; 因为这意味着要把整个 seq 都渲染完后才能知道，就没有 lazy 的意义了
        ;; 因此需要保存最大和最小的 tid
        cached-logs
        (get-in @chat-log-by-avatar [:logs (keyword (str stage-id)) (keyword (str avatar-id))])

        translator-fn (partial visualization-log-by-avatar stage-ds avatar-id)

        cache-fn #(->> %
                       (map translator-fn)
                       (filter identity))

        ;; 最后都是升序排列
        cache-append-head (cache-fn transaction-head)
        cache-append-tail (cache-fn transaction-tail)
        res (concat cache-append-head cached-logs cache-append-tail)]
    (update-logs-cache-by-avatar! stage-id avatar-id this-first-tid this-last-tid res) 
    (reverse (take limit (reverse res)))))

(defn get-no-ack-log
  [stage-id avatar-id]
  (let [stage-ds @(:history-db @(ctx-db/query-stage-db stage-id))
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
  (let [_refresh @(rf/subscribe [:partial-refresh/listen :chat-log])]
    [:div.chat-log
     [:p.chat-anchor "-- end --"]
     (cond
       (nil? observer) [:p "您没有角色在这个舞台上"]
       (= 0 observer) (query-chat-log-by-substage stage-id substage-id limit)
       :else (query-chat-log-by-avatar stage-id observer limit))]))

(defn- safe-component-mounted? [component]
  (try (boolean (rd/dom-node component)) (catch js/Object _ false)))

(defn debounce
  "Returns a function that will call f only after threshold has passed without new calls
  to the function. Calls prep-fn on the args in a sync way, which can be used for things like
  calling .persist on the event object to be able to access the event attributes in f"
  ([threshold f] (debounce threshold f (constantly nil)))
  ([threshold f prep-fn]
   (let [t (atom nil)]
     (fn [& args]
       (when @t (js/clearTimeout @t))
       (apply prep-fn args)
       (reset! t (js/setTimeout #(do
                                   (reset! t nil)
                                   (apply f args))
                                threshold))))))

(defn auto-load-chat-log-view
  [_props]
  (let [listener-fn (atom nil)
        limit (r/atom 10)

        detach-scroll-listener (fn [this]
                                 (when @listener-fn
                                   (let [node (rd/dom-node this)]
                                     (.removeEventListener node "scroll" @listener-fn)
                                     (.removeEventListener node "resize" @listener-fn)
                                     (reset! listener-fn nil))))

        should-load-more? (fn [event]
                            (let [dom-elem (.-target event)
                                  scroll-top (-> dom-elem .-scrollTop)
                                  this-height (-> dom-elem .-offsetHeight)
                                  scroll-bottom (- this-height scroll-top)
                                  total-height (-> dom-elem .-scrollHeight)
                                  threshold 50]
                              [(< (- total-height scroll-bottom) threshold) (>= scroll-top 0)]))

        scroll-listener (fn [this event]
                          (when (safe-component-mounted? this)
                            (let [[top? bottom?] (should-load-more? event)]
                              (when top?
                                (js/console.log "loading...")
                                (reset! limit (+ 10 @limit)))
                              (when bottom?
                                (let [{default-limit :limit :or {default-limit 10}} (r/props this)]
                                  (js/console.log "resetting...")
                                  (reset! limit default-limit))))))

        attach-scroll-listener (fn [this]
                                 (let [{:keys [can-show-more?]} (r/props this)
                                       node (rd/dom-node this)]
                                   (when (and can-show-more? (nil? @listener-fn))
                                     (reset! listener-fn (partial (debounce 200 scroll-listener) this))
                                     (.addEventListener node "scroll" @listener-fn)
                                     (.addEventListener node "resize" @listener-fn))))]

    (r/create-class
     {:component-did-mount
      (fn [this]
        (attach-scroll-listener this)
        (let [{default-limit :limit :or {default-limit 20}} (r/props this)]
          (reset! limit default-limit)))
      :component-did-update
      (fn [this _new-props]
        (attach-scroll-listener this))

      :component-will-umount
      (fn [this]
        (detach-scroll-listener this))

      :reagent-render
      (fn [{:keys [stage-id substage-id observer]}]
        (let [_refresh @(rf/subscribe [:partial-refresh/listen :chat-log])]
          [:div.chat-log
           [:p.chat-anchor "-- end --"]
           (either/branch
            (cond
              (nil? observer) (either/left [:p "您没有角色在这个舞台上"])
              (= 0 observer) (either/right (query-chat-log-by-substage stage-id substage-id @limit))
              :else (either/right (query-chat-log-by-avatar stage-id observer @limit)))
            (fn [left] left)
            (fn [right]
              (if (< (count right) @limit)
                (let [stage-db (ctx-db/query-stage-db stage-id)
                      stage-ds @(:history-db @stage-db)
                      first-tid (->> (d/datoms stage-ds :avet :transaction/id)
                                     first :v)]
                  (when (> first-tid 1)
                    (rf/dispatch [:play/retrieve-logs {:stage-id stage-id
                                                       :desc true
                                                       :limit 20
                                                       :begin first-tid}]))
                  (if (= first-tid 1)
                    (concat (vec right) [(with-meta [:p "已经到头了"] {:key "log-end"})])
                    right))
                right)))]))})))

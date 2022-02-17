(ns cocdan.components.chatting-log
  (:require
   [posh.reagent :as p]
   [re-posh.core :as rp]
   [reagent.core :as r]
   [reagent.dom :as d]
   [clojure.string :as str]
   [cocdan.db :as gdb]))

(defn- log-item
  [msg my-avatar-ids]
  (let [[avatar-name avatar-header avatar-attrs]
        (if (= 0 (:avatar msg))
          "system"
          (->> @(p/q '[:find ?name ?header ?attrs
                       :in $ ?avatar-id
                       :where
                       [?e :avatar/id ?avatar-id]
                       [?e :avatar/name ?name]
                       [?e :avatar/header ?header]
                       [?e :avatar/attributes ?attrs]]
                     gdb/conn (:avatar msg))
               (reduce into [])))
        {msg-text :msg
         msg-type :type
         avatar-id :avatar} msg
        address-info (let [items (->> avatar-attrs :coc :items)
                           hands (->> (reduce (fn [a [k vs]]
                                                (if (contains? #{:左手持 :右手持 :双手持} k)
                                                  (conj a (reduce (fn [a {hidden? :hidden?
                                                                          name :name}]
                                                                    (if (= hidden? "显露")
                                                                      (conj a name)
                                                                      a)) [] vs))
                                                  a)) [] items)
                                      flatten)
                           res (-> ""
                                   (#(if (empty? hands)
                                       %
                                       (str % "手持" (str/join "," hands)))))]
                       res)]
    (cond
      (= msg-type "action") [:p {:style {:padding-top "1em" :padding-bottom "1em"}
                                 :class "has-text-centered"} [:i (str "--- " avatar-name " " msg-text " ---")]]
      (= msg-type "system-msg") [:p {:style {:padding-top "1em"
                                             :padding-bottom "1em"
                                             :font-size "14px"}
                                     :class "has-text-centered"} [:i msg-text]]

      (contains? my-avatar-ids avatar-id)
      [:div.is-flex.is-justify-content-end
       [:div.media
        {:style {:padding-right "1em"
                 :max-width "40em"}}
        [:div.media.media-content
         [:div.content
          [:div
           [:p.is-flex.is-justify-content-end [:strong avatar-name] [:span {:style {:font-size 12
                                                                                    :margin-top "5px"
                                                                                    :margin-left "5px"}} address-info]]
           [:div.has-background-white-ter
            {:style {:border-radius "0.5em"
                     :padding "0.75em"}}
            msg-text]]]]
        [:figure.media-right
         [:p.image.is-64x64
          [:img {:style {:height "100%"
                         :object-fit "cover"}
                 :src avatar-header}]]]]]

      :else
      [:div.is-flex
       [:div.media
        {:style {:padding-bottom "1em"
                 :max-width "40em"}}
        [:figure.media-left
         [:p.image.is-64x64
          [:img {:style {:height "100%"
                         :object-fit "cover"}
                 :src avatar-header}]]]
        [:div.media.media-content
         [:div.content
          [:div
           [:strong avatar-name]
           [:div.has-background-white-ter
            {:style {:border-radius "0.5em"
                     :padding "0.75em"}}
            msg-text]]]]]])))

(defn- get-msgs
  [ds current-use-avatar limit]
  (let [ids (->> @(p/q '[:find ?id
                         :in $ ?current-use-avatar-id
                         :where
                         [?id :message/receiver ?current-use-avatar-id]]
                       ds
                       current-use-avatar)
                 (reduce into []))
        msgs @(p/pull-many ds '[*] ids)]
    (->> (gdb/remove-db-perfix msgs)
         (sort-by :time)
         reverse
         (take limit)
         reverse)))

(comment
  (get-msgs gdb/conn 3 1)
  )

(defn chatting-log
  [_stage-id avatar-id _my-avatars]

  (r/with-let [always-bottom (r/atom true)
               limit (r/atom 40)
               msgs (r/atom [])]
    (r/create-class
     {:display-name "chatting-log"

      :component-did-mount
      (fn [this]
        (let [this-node (d/dom-node this)
              p-node (.-parentNode this-node)]
          (set! (.-scrollTop p-node) (.-scrollHeight p-node))
          (reset! msgs (get-msgs gdb/conn avatar-id @limit))
          (.addEventListener
           p-node
           "scroll"
           #(if (> (+ (.-scrollTop p-node) (.-clientHeight p-node)) (- (.-clientHeight this-node) 200))
              (do
                (reset! always-bottom true)
                (reset! limit 40))
              (reset! always-bottom false)))))

      :component-did-update
      (fn [this _old-argv]
        (let [[_stage-id avatar-id _my-avatars] (rest (r/argv this))
              this-node (d/dom-node this)
              p-node (.-parentNode this-node)
              {last-time :time} (last (get-msgs gdb/conn avatar-id 10))]
          (when (not (nil? last-time))
            (rp/dispatch [:rpevent/upsert :avatar {:id avatar-id :latest-read-message-time last-time}]))
          (when @always-bottom
            (set! (.-scrollTop p-node) (.-scrollHeight p-node)))))

      :reagent-render
      (fn [_stage-id avatar-id my-avatars]
        (let [my-avatar-ids (set (for [avatar my-avatars]
                                   (:id avatar)))
              msgs (get-msgs gdb/conn avatar-id @limit)]
          [:div
           {:style {:width "100%"
                    :padding-bottom "1em"}}
           [:p {:style {:padding-top "1em" :padding-bottom "1em"}
                :class "has-text-centered"
                :on-click #(swap! limit (fn [x] (+ x 40)))} [:i "加载更多消息"]]
           (doall (for [msg msgs]
                    (with-meta (log-item msg my-avatar-ids) {:key (str "log-" (:time msg) (:receiver msg))})))]))})))
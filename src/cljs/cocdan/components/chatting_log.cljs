(ns cocdan.components.chatting-log
  (:require
   [re-posh.core :as rp]
   [reagent.core :as r]
   [reagent.dom :as d]
   [clojure.string :as str]
   [cocdan.core.log :refer [posh-avatar-latest-message-time query-latest-messages-by-avatar-id get-avatar-from-ctx]]
   [cocdan.db :as gdb]))

(defn- log-item
  [{msg-text :content
    msg-type :type
    ctx-eid :ctx
    avatar-id :sender :as _msg} current-use-avatar-id]
  (let [{avatar-attrs :attributes
         avatar-header :header
         avatar-name :name} (get-avatar-from-ctx ctx-eid avatar-id)
        address-info (let [items (->> avatar-attrs :coc :items)
                           hands (->> (reduce (fn [a [k vs]]
                                                (if (contains? #{:左手持 :右手持 :双手持} k)
                                                  (conj a (reduce (fn [a {hidden? :hidden?
                                                                          name :name}]
                                                                    (if (= hidden? "显露")
                                                                      (conj a name)
                                                                      a)) [] vs))
                                                  a)) [] items)
                                      flatten)]
                       (when (seq hands)
                         (str "手持" (str/join "," hands))))]
    (cond
      (= msg-type "use-items") [:p {:style {:padding-top "1em" :padding-bottom "1em"}
                                 :class "has-text-centered"} [:i (str "--- " avatar-name " " msg-text " ---")]]
      (= msg-type "system-msg") [:p {:style {:padding-top "1em"
                                             :padding-bottom "1em"
                                             :font-size "14px"}
                                     :class "has-text-centered"} [:i msg-text]]

      (= current-use-avatar-id avatar-id)
      [:div.is-flex.is-justify-content-end
       [:div.media
        {:style {:padding-right "1em"
                 :max-width "40em"}}
        [:div.media.media-content
         [:div.content
          [:div
           [:p.is-flex.is-justify-content-end
            [:strong avatar-name]
            [:span {:style {:font-size 12
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
           [:p.is-flex.is-justify-content-start [:strong avatar-name] [:span {:style {:font-size 12
                                                                                      :margin-top "5px"
                                                                                      :margin-left "5px"}} address-info]]
           [:div.has-background-white-ter
            {:style {:border-radius "0.5em"
                     :padding "0.75em"}}
            msg-text]]]]]])))

(defn chatting-log
  [_stage _avatar-id _my-avatars]

  (r/with-let [always-bottom (r/atom true)
               limit (r/atom 40)]
    (r/create-class
     {:display-name "chatting-log"

      :component-did-mount
      (fn [this]
        (let [this-node (d/dom-node this)
              p-node (.-parentNode this-node)]
          (set! (.-scrollTop p-node) (.-scrollHeight p-node))
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
        (let [[_stage avatar-id _my-avatars] (rest (r/argv this))
              this-node (d/dom-node this)
              p-node (.-parentNode this-node)
              {last-time :time} (last (query-latest-messages-by-avatar-id gdb/db avatar-id 10))]
          (when (not (nil? last-time))
            (rp/dispatch [:rpevent/upsert :avatar {:id avatar-id :latest-read-message-time last-time}]))
          (when @always-bottom
            (set! (.-scrollTop p-node) (.-scrollHeight p-node)))))

      :reagent-render
      (fn [_stage current-use-avatar-id _my-avatars]
        (let [_latest-time @(posh-avatar-latest-message-time gdb/db current-use-avatar-id)] ;; trigger re-rendering
          [:div
           {:style {:width "100%"
                    :padding-bottom "1em"}}
           [:p {:style {:padding-top "1em" :padding-bottom "1em"}
                :class "has-text-centered"
                :on-click #(swap! limit (fn [x] (+ x 40)))} [:i "加载更多消息"]]
           (doall (for [msg (query-latest-messages-by-avatar-id gdb/db current-use-avatar-id @limit)]
                    (with-meta (log-item msg current-use-avatar-id) {:key (str "log-" (:time msg) (:receiver msg))})))]))})))
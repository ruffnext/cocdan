(ns cocdan.components.chatting-log
  (:require
   [cocdan.db :refer [conn]]
   [posh.reagent :as p]
   [re-posh.core :as rp]
   [reagent.core :as r]
   [reagent.dom :as d]))


(defn- log-item
  [msg my-avatar-ids]
  (let [[avatar-name avatar-header] (if (= 0 (:avatar msg))
                                      "system"
                                      (->> @(p/q '[:find ?name ?header
                                                   :in $ ?avatar-id
                                                   :where
                                                   [?e :avatar/id ?avatar-id]
                                                   [?e :avatar/name ?name]
                                                   [?e :avatar/header ?header]]
                                                 conn (:avatar msg))
                                           (reduce into [])))
        {msg-text :msg
         msg-type :type
         avatar-id :avatar} msg]
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
           [:strong.is-flex.is-justify-content-end avatar-name]
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

(defn chatting-log
  [_logs _my-avatars]

  (r/with-let [always-bottom (r/atom true)]
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
              (reset! always-bottom true)
              (reset! always-bottom false)))))

      :component-did-update
      (fn [this _old-argv]
        (let [[logs _my-avatars] (rest (r/argv this))
              this-node (d/dom-node this)
              p-node (.-parentNode this-node)
              {last-time :time
               receiver :receiver} (last logs)]
          (when (not (nil? last-time))
            (rp/dispatch [:rpevent/upsert :avatar {:id receiver :latest-read-message-time last-time}]))
          (when @always-bottom
            (set! (.-scrollTop p-node) (.-scrollHeight p-node)))))

      :reagent-render
      (fn [msgs my-avatars]
        (let [my-avatar-ids (set (for [avatar my-avatars]
                                   (:id avatar)))]
          [:div
           {:style {:width "100%"
                    :padding-bottom "1em"}}
           (doall (for [msg msgs]
                    (with-meta (log-item msg my-avatar-ids) {:key (str "log-" (:time msg) (:receiver msg))})))]))})))


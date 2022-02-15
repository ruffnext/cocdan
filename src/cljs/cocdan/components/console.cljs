(ns cocdan.components.console 
  (:require [reagent.core :as r]
            [clojure.string :as s]
            [cocdan.network.ws :as ws]
            [cocdan.auxiliary :as aux]
            [cljs-http.client :as http]))

(def shellHolder "> ")
(def textInputed (r/atom shellHolder))
(def textHistory (r/atom []))

(defn addTextToConsole
  [x] (swap! textHistory #(conj % x)) nil)

(defn- command-router
  [cmd]
  (cond
    (s/blank?  cmd) ""
    (s/starts-with? cmd ".connect ") (ws/make-websocket!
                                      (s/replace-first cmd ".connect " "")
                                      #(-> % .-data addTextToConsole)
                                      (fn [_] (addTextToConsole "Connection Closed")))
    (s/starts-with? cmd ".login") (let [rest (s/replace-first cmd ".login" "")
                                        [_ name email] (s/split rest #" ")]
                                    (cond
                                      (s/blank? email) (addTextToConsole "usage: \n   .login <name> <email> \n")
                                      :else (aux/request! http/post "/api/user/login" {:email email
                                                                                        :name name} #(addTextToConsole (str (:body %))))))
    (s/starts-with? cmd ".logout") (aux/request! http/delete "/api/user/logout" {} #(cond
                                                                                       (not= 200 (:status %)) (addTextToConsole (str (:body %)))
                                                                                       :else nil))
    (s/starts-with? cmd ".clear") (reset! textHistory [])
    :else (ws/send-transit-msg! cmd)))

(defn- on-key-press
  [e]
  (when (= 13 (.-charCode e))
    (swap! textHistory conj @textInputed)
    (let [cmd (s/replace-first @textInputed shellHolder "")
          cmdres (command-router cmd)]
      (when (and (string? cmdres) (not (s/blank? cmdres))) (swap! textHistory #(conj % cmdres))))
    (reset! textInputed shellHolder)))

(defn- on-change
  [event]
  (let [text (-> event
                 .-target
                 .-value)]
    (cond (>= (count text) (count shellHolder)) (reset! textInputed text))))

(defn cmd-page []
  [:section.section>div.container>div.content
   [:div {:id "console-container"}
    (map-indexed (fn [i x]  ^{:key (str "history-" i)}
                   [:p {:style {:margin 0}
                        :class "console-log"} x]) @textHistory)
    [:input {:value @textInputed
             :type "text"
             :style {:border "none"
                     :outline "medium"
                     :width "100%"
                     :overflow "hidden"}
             :class "console-input"
             :on-change on-change
             :on-key-press on-key-press}]]])
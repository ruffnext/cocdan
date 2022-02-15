(ns cocdan.modals.stage-find
  (:require
   [cocdan.auxiliary :as aux]
   [reagent.core :as r]
   [clojure.string :as str]
   [cljs-http.client :as http]
   [clojure.core.async :refer [go <!]]
   [markdown.core :refer [md->html]]
   [re-frame.core :as rf]
   [cats.monad.either :as either]
   [cats.core :as m]))

(defonce active? (r/atom false))
(defonce join-status (r/atom "is-disabled"))
(defonce search-status (r/atom "fa-search"))
(defonce input-search (r/atom ""))
(defonce select-avatar (r/atom 0))
(defonce avatar-name-input (r/atom ""))
(defonce avatar-name-check-msg (r/atom ""))
(defonce avatar-transform-option (r/atom "copy"))
(defonce avatar-transform-limit (r/atom false))
(defonce stage (r/atom nil))
(defonce stage-avatars (r/atom nil))

(aux/init-page
 {}
 {:event/modal-find-stage-active (fn [{:keys [db]}]
                                   (swap! active? not)
                                   {:db db})})

(defn- close-modal
  []
  (reset! active? false))

(defn- on-cancel
  []
  (reset! active? false)
  (reset! stage nil)
  (reset! join-status "is-disabled"))

(defn- do-search
  []
  (go (let [res (<! (http/get "api/stage/get-by-code" {:query-params {:code @input-search}}))]
        (cond
          (= 200 (:status res)) (do
                                  (reset! stage (:body res))
                                  (reset! join-status "is-primary"))
          :else (js/console.log res)))))

(declare option-check)

(defn- refresh-stage-avatars
  []
  (go (let [res (<! (http/get (str "api/stage/s" (:id @stage) "/list/avatar") {:query-params {:code @input-search}}))]
        (cond
          (= 200 (:status res)) (do
                                  (reset! stage-avatars (:body res))
                                  (option-check))))))

(defn- generate-avatar-select-option
  [avatar]
  [:option {:value (:id avatar)} (str (:name avatar) (if (nil? (:on_stage avatar))
                                                       ""
                                                       (let [stage @(rf/subscribe [:subs/general-get-stage-by-id (:on_stage avatar)])]
                                                         (str " @ " (:title stage)))))])

(defn- join-stage
  []
  (reset! join-status "is-loading")
  (go
    (let [avatar-created (cond
                           (= 0 @select-avatar) (let [res (<! (http/post "api/avatar/create" {:json-params {:name @avatar-name-input}}))]
                                                  (if (= 201 (:status res))
                                                    (:body res)
                                                    (js/console.log res)))
                           (= "copy" @avatar-transform-option) (let [res (<! (http/post (str "api/avatar/a" @select-avatar "/duplicate")))]
                                                                 (if (= 201 (:status res))
                                                                   (:body res)
                                                                   (js/console.log res)))
                           (= "move" @avatar-transform-option) {:id @select-avatar})
          _ (js/console.log avatar-created)
          stage-joined (when (not (nil? avatar-created))
                         (let [res (<! (http/post "api/stage/join-by-code" {:json-params {:avatar (:id avatar-created)}
                                                                             :query-params {:code @input-search}}))]
                           (if (= 200 (:status res))
                             res
                             (js/console.log res))))]
      (if (nil? stage-joined)
        (reset! join-status "is-danger")
        (do
          (reset! join-status "is-success")
          (rf/dispatch [:event/avatar-refresh])
          (js/setTimeout #(reset! active? false) 1000))))))


(defn- check-name-len
  [name]
  (let [errmsg (cond
                 (str/blank? name) "name is required"
                 (< (count name) 2) "name is too short"
                 :else "")]
    (if (str/blank? errmsg)
      (either/right "")
      (do 
        (reset! avatar-name-check-msg errmsg)
        (either/left errmsg)))))

(defn- check-name-conflict
  [name stage-avatars]
  (let [res (filter #(= (str/lower-case (:name %)) name) stage-avatars)]
    (if (empty? res)
      (do
        (reset! avatar-name-check-msg "")
        (either/right ""))
      (do
        (reset! avatar-name-check-msg "conflict with character name on stage")
        (either/left "")))))

(defn- check-transform-option
  []
  (let [kp-list (set (reduce (fn [a x] (conj a (:owned_by x))) [] @(rf/subscribe [:subs/general [:stages]])))]
    (if (contains? kp-list @select-avatar)
      (do
        (reset! avatar-transform-option "copy")
        (reset! avatar-transform-limit true))
      (reset! avatar-transform-limit false))))

(defn- option-check
  []
  (when (nil? @stage-avatars) (refresh-stage-avatars))
  (reset! join-status "is-disabled")
  (check-transform-option)
  (m/mlet [name (either/right (str/lower-case @avatar-name-input))
           _ (m/do-let (check-name-len name)
                       (check-name-conflict name @stage-avatars))]
          (reset! join-status "is-primary")))

(defn- move-button
  [disabled?]
  (let [[title class] (if disabled?
                        ["The avatar cannot be moved because it is the stage host " "is-disabled"]
                        ["this will move the avatar to this stage from previous one" (if (= @avatar-transform-option "move") 
                                                                                       "is-primary"
                                                                                       "")])]
    [:button.button {:title title
                     :on-click #(reset! avatar-transform-option "move")
                     :class class
                     :disabled disabled?} "Move"]))

(defn- on-avatar-change
  [id]
  (reset! select-avatar id)
  (if (not= 0 id)
    (reset! avatar-name-input (:name @(rf/subscribe [:subs/general-get-avatar-by-id id])))
    (reset! avatar-name-input ""))
  (option-check))

(defn- modal-body
  [stage]
  [:div.modal-card-body
   [:div.content
    {:dangerouslySetInnerHTML {:__html (md->html (:introduction stage))}}]
   [:hr]
   [:label.label "Options"
    [:div.field {:class "is-horizontal"}
     [:div {:class "field-label is-normal"}
      [:label.label "Avatar"]]
     [:div.field-body>div.field>div.control>div.select
      [:select
       {:class "modal-stage-input"
        :on-change #(on-avatar-change (js/parseInt (-> % .-target .-value)))}
       [:option {:value 0} "create a new avatar"]
       (doall
        (map-indexed 
         (fn [i x] (with-meta (generate-avatar-select-option x) {:key (str "ss" i)}))
         @(rf/subscribe [:subs/general-get-my-avatars])))]]]
    [:div.field {:class "is-horizontal"}
     [:div {:class "field-label is-normal"}
      [:label.label " Name"]]
     [:div.field-body>div.field {:class "has-addons"}
      [:div.control {:class "modal-stage-input has-icons-right"}
       [:input {:class (str "input " (if (str/blank? @avatar-name-check-msg)
                                       "is-primary"
                                       "is-danger"))
                :type "text"
                :placeholder "Your Name"
                :on-change #(do
                              (reset! avatar-name-input (-> % .-target .-value))
                              (option-check))
                :value @avatar-name-input}]
       [:span {:class "icon is-small is-right"}
        [:i {:class (if (str/blank? @avatar-name-check-msg)
                      "fas fa-check"
                      "fas fa-times")}]]]
      [:div.control {:class "columns is-vcentered"}
       (if (str/blank? @avatar-name-check-msg)
         nil
         [:p {:style {:padding-left "3em"}
              :class "help is-danger"} @avatar-name-check-msg])]]]
    (when (not= 0 @select-avatar)
      [:div.field {:class "is-horizontal"}
       [:div {:class "field-label is-normal"}
        [:label.label "Transform"]]
       [:div.field-body
        [:div {:class "buttons has-addons"}
         [:button.button {:title "this will make a copy of avatar and join this stage"
                          :on-click #(reset! avatar-transform-option "copy")
                          :class (if (= @avatar-transform-option "copy") 
                                   "is-primary"
                                   "")} "Copy"]
         (move-button @avatar-transform-limit)
         ]]])]])


(defn stage-find
  []
  [:div.modal {:class (if @active? "is-active" "")}
   [:div.modal-background {:on-click close-modal}]
   [:div.modal-card
    [:header.modal-card-head
     [:span {:class "subtitle is-5" :style {:width "10em"}} "Search Stage"]
     [:div {:class "field has-addons"}
      [:div.control
       [:input.input {:placeholder "paste code here"
                      :value @input-search
                      :on-change #(reset! input-search (-> % .-target .-value))}]]
     [:div.control
      [:button.button {:style {:margin-right "8em"}
                       :on-click do-search}
       [:span {:class "icon is-small"}
        [:i {:class (str "fa " @search-status)}]]]]]
     [:button.delete {:on-click close-modal}]]
    (when (not (nil? (:banner @stage)))
      (list
       (with-meta [:div.modal-card-image {:class "modal-stage-banner-img"}
                   [:img {:src (:banner @stage)
                          :class "modal-stage-banner-img"
                          :style {:height "100%" 
                                  :width "100%"
                                  :object-fit "cover"}}]] {:key "sfmsbi"})
       (with-meta (modal-body @stage) {:key "sfmsbd"})))
    [:footer.modal-card-foot
     [:button.button (merge {:on-click join-stage :class @join-status} (if (= @join-status "is-disabled")
                                                                         {:disabled true}
                                                                         {})) "Join"]
     [:button.button {:on-click on-cancel} "close"]]
    [:button {:class "modal-close is-large" :on-click close-modal}]]])
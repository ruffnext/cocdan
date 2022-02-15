(ns cocdan.modals.general-attr-editor
  (:require
   [reagent.core :as r]
   [cocdan.auxiliary :as aux]
   [re-frame.core :as rf]))

(defonce active? (r/atom false))
(defonce attrs (r/atom nil))
(defonce col-keys (r/atom nil))
(defonce base-key (r/atom nil))
(defonce schema (r/atom {}))

(sort (:substage @schema))

(aux/init-page
 {}
 {:event/modal-general-attr-editor-active
  (fn [_ _ base-key' col-keys' attrs' schema']
    (if (and
         (not (nil? (:id attrs')))
         (contains? #{:avatar :stage} base-key'))
      (do (reset! active? true)
          (reset! attrs attrs')
          (reset! base-key base-key')
          (reset! col-keys col-keys')
          (if (nil? schema')
            (reset! schema (reduce (fn [a f]
                                     (f a)) attrs' col-keys'))
            (reset! schema schema')))
      (do
        (js/console.log "no id")
        (js/console.log attrs')))
    {})})


(defn- edit-cancel
  []
  (reset! active? false))


(defn- check-list-candidate
  [path default-value v-schema]
  (cond
    (set? v-schema) (let [filtered-default-value (set (filter #(contains? v-schema %) default-value))]
                      (when (not= (set default-value) (set filtered-default-value))
                        (swap! attrs (fn [a] (assoc-in a (conj @col-keys (keyword path)) filtered-default-value)))
                        filtered-default-value))
    (or
     (vector? v-schema)
     (seq? v-schema)) (if (contains? (set v-schema) default-value)
                        (.indexOf v-schema default-value)
                        (do
                          (swap! attrs (fn [a] (assoc-in a (conj @col-keys (keyword path)) (first v-schema))))
                          (first v-schema)))
    :else default-value))



(defn- edit-item
  [path default-value' v-schema]
  (let [default-value (check-list-candidate path default-value' v-schema)
        key-path (conj @col-keys (keyword path))]
    [:div {:class "field is-horizontal"}
     [:div {:class "field-label is-normal"}
      [:label (str path)]]
     [:div.field-body>div.field>div.control
      (cond
        (string? v-schema) [:input.input
                            {:placeholder "string"
                             :defaultValue default-value
                             :on-change #(swap! attrs (fn [a] (assoc-in a key-path (-> % .-target .-value))))}]
        (int? v-schema) [:input.input
                         {:placeholder v-schema
                          :defaultValue default-value
                          :type "number"
                          :on-change #(swap! attrs (fn [a] (assoc-in a key-path (js/parseInt (-> % .-target .-value)))))}]
        (float? v-schema) [:input.input
                           {:placeholder v-schema
                            :defaultValue default-value
                            :type "number"
                            :on-change #(swap! attrs (fn [a] (assoc-in a key-path (js/parseFloat (-> % .-target .-value)))))}]
        (set? v-schema) [:div.select {:class "is-multiple"}
                         [:select {:multiple true
                                   :size 8
                                   :style {:max-height "10em"}
                                   :value (let [val (reduce (fn [a f] (f a)) @attrs key-path)
                                                seqval (seq val)]
                                            (if (nil? seqval)
                                              []
                                              seqval))
                                   :on-change #()
                                   :on-click (fn [e]
                                               (let [value (-> e .-target .-value)
                                                     ori-value (set (reduce (fn [a f] (f a)) @attrs key-path))]
                                                 (if (contains? ori-value value)
                                                   (swap! attrs (fn [a] (assoc-in a key-path (disj ori-value value))))
                                                   (swap! attrs (fn [a] (assoc-in a key-path (conj ori-value value)))))))}
                          (doall (map
                                  (fn [v] (with-meta [:option
                                                      (str v)] {:key (str "gae-s-" v)}))
                                  v-schema))]]
        (seq v-schema) [:div.select
                        [:select
                         {:default-value default-value
                          :on-change #(swap! attrs (fn [a]
                                                     (js/console.log (assoc-in a key-path
                                                                               (nth v-schema (js/parseInt (-> % .-target .-value)))))
                                                     (assoc-in a key-path
                                                               (nth v-schema (js/parseInt (-> % .-target .-value))))))}
                         (doall (map-indexed
                                 (fn [i v] (with-meta [:option
                                                       {:value i}
                                                       (str v)] {:key (str "gae-s-" i)}))
                                 v-schema))
                         (when (< (count v-schema) default-value)
                           (swap! attrs (fn [a] (assoc-in a key-path (first v-schema)))))]])]]))


(defn general-attr-editor
  []
  (when (and @active? (not (nil? @attrs)))
    [:div.modal {:class "is-active"}
     [:div.modal-background {:on-click edit-cancel}]
     [:div.modal-card
      [:header.modal-card-head
       [:p "general attribute editor for " (str @col-keys)]]
      [:section.modal-card-body
       (let [value (reduce (fn [a f] (f a)) @attrs @col-keys)]
         (doall (for [[k v-schema] @schema]
                  (with-meta (edit-item k (k value) v-schema) {:key (str "gae-" k)}))))]
      [:footer.modal-card-foot
       [:button.button {:class "is-primary"
                        :on-click #(do
                                     (rf/dispatch [:event/patch-to-server @base-key (assoc-in
                                                                                     {:id (:id @attrs)}
                                                                                     @col-keys
                                                                                     (reduce (fn [a f] (f a)) @attrs @col-keys))])
                                     (edit-cancel))} "Submit"]
       [:button.button {:on-click edit-cancel} "Cancel"]]]]))

(ns cocdan.pages.avatar 
  (:require
   [cocdan.db :refer [db]]
   [cocdan.core.avatar :refer [default-avatar get-avatar-attr]]
   [cocdan.core.coc :refer [complete-coc-avatar-attributes
                            get-coc-attr]]
   [reagent.core :as r]
   [datascript.core :as d]
   [clojure.core.async :refer [go <!]]
   [cljs-http.client :as http]
   [cocdan.auxiliary :refer [remove-db-perfix]]
   [re-frame.core :as rf]
   [cocdan.components.coc.equipment-editor :refer [coc-equipment-editor]]
   [cocdan.components.coc.avatar-skill-edit :refer [coc-skill-editor]]
   [cocdan.components.coc.avatar-basic-info-edit :refer [coc-avatar-basic-info-editor]]
   [cocdan.components.coc.avatar-basic-attr-edit :refer [coc-avatar-basic-attr-edit]]
   [cocdan.components.click-upload-img :refer [click-upload-img]]))

(defn page
  [{id :id}]
  (r/with-let [avatar (r/atom default-avatar)
               check-avatar (r/atom {})
               submit (fn [avatar]
                        (case (:id avatar)
                          "0" (rf/dispatch [:http-event/create-avatar avatar])
                          (rf/dispatch [:event/patch-to-server :avatar avatar])))]
    (r/create-class
     {:display-name "avatar-page"

      :component-did-update
      (fn [this [_ {old-avatar-id :id}]]
        (let [[{new-avatar-id :id}] (rest (r/argv this))]
          (when (not= old-avatar-id new-avatar-id)
            (let [avatar-id (js/parseInt new-avatar-id)
                  eid (d/entid @db [:avatar/id avatar-id])
                  new-avatar (cond
                               (= 0 avatar-id) (complete-coc-avatar-attributes {} default-avatar)
                               (not (nil? eid)) (complete-coc-avatar-attributes @avatar (remove-db-perfix (d/pull @db '[*] eid)))
                               :else nil)]
              (if new-avatar
                (reset! avatar new-avatar)
                (go (let [res (<! (http/get (str "/api/avatar/a" avatar-id)))]
                      (when (= (:status res) 200)
                        (reset! avatar (:body res))))))))))

      :component-did-mount
      (fn [_this]
        (let [avatar-id (js/parseInt id)
              eid (d/entid @db [:avatar/id avatar-id])]
          (if (and (nil? eid) avatar-id)
            (go (let [res (<! (http/get (str "/api/avatar/a" avatar-id)))]
                  (when (= (:status res) 200)
                    (reset! avatar (complete-coc-avatar-attributes (:body res) (:body res))))))
            (reset! avatar (-> (d/pull @db '[*] eid) remove-db-perfix (#(complete-coc-avatar-attributes  % %)))))
          (when (not (d/q '[:find ?skill-name .
                            :where [?eid :coc-skill/skill-name ?skill-name]]
                          @db))
            (rf/dispatch [:coc-event/refresh-skills])) ; refresh skills first
          (when (not (d/q '[:find ?occupation-name .
                            :where [?eid :coc-occupation/occupation-name ?occupation-name]]
                          @db))
            (rf/dispatch [:coc-event/refresh-occupations]))))

      :reagent-render
      (fn [{_id :id}]
        [:div.container>div.section>div.card
         [:div.container
          {:style {:padding-top "2em"
                   :padding-bottom "0em"
                   :padding-left "1em"
                   :padding-right "2em"}}
          [:div.columns.is-horizontal
           [:div.column.has-text-centered.nested-column.is-half
            [coc-avatar-basic-info-editor avatar check-avatar]]
           [:div.column.has-text-centered.nested-column.is-one-third
            [coc-avatar-basic-attr-edit avatar]]
           [:div.column.has-text-centered.sketch {:style {:width "20%"}}
            [click-upload-img {:style {:width "100%"
                                       :height "100%"}} (:header @avatar)
             {:on-uploaded #(swap! avatar (fn [x] (assoc x :header %)))}]]]
          [:div.columns.is-horizontal
           [:div.column.is-one-third.has-text-centered>div.sketch
            {:style {:height "4em"}}
            (let [healthy (str (get-coc-attr @avatar :healthy))]
              [:p.icon.fas.fa-briefcase-medical.fa-2x
               {:class (if (not= "健康" healthy) "has-text-danger" "")
                :title (str (get-coc-attr @avatar :healthy))
                :style {:height "100%"
                        :width "100%"
                        :line-height "1.8em"}}
               [:span {:style {:padding-left "1em"}} (str "HP " (get-coc-attr @avatar :hp) " / " (get-coc-attr @avatar :max-hp))]])]
           [:div.column.is-one-third.has-text-centered>div.sketch
            {:style {:height "4em"}}
            (let [sanity (str (get-coc-attr @avatar :sanity))]
              [:p.icon.fa.fa-brain.fa-2x
               {:class (if (not= "清醒" sanity) "has-text-danger" "")
                :title (str (get-coc-attr @avatar :sanity))
                :style {:height "100%"
                        :width "100%"
                        :line-height "1.8em"}}
               [:span {:style {:padding-left "0.75em"}} (str "SAN " (get-coc-attr @avatar :san) " / " (get-coc-attr @avatar :max-san))]])]
           [:div.column.is-one-third.has-text-centered>div.sketch
            {:style {:height "4em"}}
            (let [mp (get-coc-attr @avatar :mp)
                  max-mp (get-coc-attr @avatar :max-mp)]
              [:p.icon.fa.fa-hat-wizard.fa-2x
               {:class (if (and (int? mp) (int? max-mp) (< mp (/ max-mp 2))) "has-text-danger" "")
                :style {:height "100%"
                        :width "100%"
                        :line-height "1.8em"}}
               [:span {:style {:padding-left "0.75em"}} (str "MP " (get-coc-attr @avatar :mp) " / " (get-coc-attr @avatar :max-mp))]])]]
          [:hr]
          [:p.title.is-4.has-text-centered "角色技能"]
          [coc-skill-editor avatar]
          [:hr]
          [:p.title.is-4.has-text-centered "背景故事"]
          [:div.column
           [:div.field
            [:div.control
             [:textarea.textarea 
              {:onBlur #(swap! avatar (fn [a] (assoc-in a [:attributes :background-story] (-> % .-target .-value))) )
               :placeholder "请填写角色的背景故事"
               :defaultValue (get-avatar-attr @avatar [:background-story])}]]]]
          [:hr]
          [:p.title.is-4.has-text-centered "携带的物品"]
          [coc-equipment-editor {:on-change (fn [loc-key items]
                                              (swap! avatar (fn [x]  (assoc-in x [:attributes :coc :items loc-key]
                                                                               items))))} @avatar]
          [:footer.card-footer
           {:style {:margin-top "2em"
                    :padding-top "1em"
                    :padding-bottom "1em"}}
           [:a.card-footer-item.title.is-4.has-text-info
            {:on-click #(submit @avatar)
             :style {:margin-bottom "0px"}}
            "Save"]
           [:a.card-footer-item.title.is-4.has-text-danger
            {:title "这个按钮的逻辑还没有写"
             :on-click #()}
            "Delete"]]]])})))

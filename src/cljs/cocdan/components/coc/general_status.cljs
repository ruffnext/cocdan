(ns cocdan.components.coc.general-status
  (:require
   [re-frame.core :as rf]
   [clojure.string :as str]
   [cocdan.db :as gdb]
   [cocdan.core.stage :refer [posh-am-i-stage-admin?]]))

(defn- remove-perfix
  [substage-name current-substage-name]
  (if (str/starts-with? substage-name (str current-substage-name ">"))
    (str/replace-first substage-name (str current-substage-name ">") "")
    substage-name))

(defn general-status
  [stage avatar]
  (let [substages (-> stage :attributes :substages)
        substage-id (-> avatar :attributes :substage)
        _avatar-coc-attr (-> avatar :attributes :coc)
        {substage-name :name
         {acoustic :声音
          vision :视线
          connected :连通区域} :coc} (if (nil? substage-id)
                                   nil
                                   ((keyword substage-id) substages))
        _can-edit (= (:owned_by stage) (:id avatar))
        am-i-stage-admin? @(posh-am-i-stage-admin? gdb/db (:id stage))
        on-substage-info-click (if am-i-stage-admin?
                                 #(rf/dispatch
                                   [:event/modal-general-attr-editor-active
                                    :stage
                                    [:attributes :substages (keyword substage-id) :coc]
                                    stage
                                    {:声音 0 :视线 0 :连通区域 (disj (set (for [[i _v] substages]
                                                                    (name i)))
                                                             (name substage-id))}])
                                 #())
        on-substage-name-click (if am-i-stage-admin?
                                 #(rf/dispatch [:event/modal-substage-edit-active stage substage-id])
                                 #())]
    
    [:div
     [:p.has-text-centered {:on-click on-substage-name-click
                            :style {:margin-top "6px"}}
      [:strong  substage-name]]
     (when (not (nil? substage-id))
       [:div.columns {:style {:margin-bottom "0px"}
                      :on-click on-substage-info-click}
        [:div.column {:style {:margin "6px"}}
         [:p (str "声音：" (cond
                          (< acoustic 10) "寂静"
                          (< acoustic 20) "安静"
                          (< acoustic 30) "嘈杂"
                          (< acoustic 60) "喧闹"
                          (< acoustic 80) "震耳欲聋"
                          :else "难以忍受"))]]
        [:div.column {:style {:margin "6px"}}
         [:p (str "视线：" (cond
                          (< vision 30) "清晰"
                          (< vision 60) "受阻"
                          (< vision 90) "遮蔽"
                          :else "难以视物"))]]])
     [:div
      {:style {:margin-left "0.5em"}}
      [:p {:style {:margin-bottom "0.25em"}} "可到达的区域："]
      [:div.tags.is-normal {:style {:margin-left "1em"
                                    :margin-bottom "1em"}}
       (doall (map (fn [n]
                     (with-meta [:span.tag
                                 (-> ((keyword n) substages)
                                     :name
                                     (remove-perfix substage-name))] {:key (str "gsc-" n)})) connected))]]]))
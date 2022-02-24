(ns cocdan.components.click-upload-img 
  (:require
   [cocdan.auxiliary :as aux]
   [reagent.core :as r]
   [reagent.dom :as d]
   [cljs-http.client :as http]
   [clojure.core.async :refer [go <!]]))

(def upload-url "/api/files/upload")

(defn- handle-upload
  []
  ())

(defn- upload-img-changed
  [_ _ res _]
  {:http-request {:url upload-url
                        :method http/post
                        :multipart-params ["file" res]
                        :resp-event handle-upload}})

(aux/init-page
 {}
 {:event/click-upload-img-changed upload-img-changed})

(defn click-upload-img
  [_ _ {on-uploaded :on-uploaded}]
  (let [handle-changed (fn [new-file]
                         (go (let [res  (<! (http/post upload-url
                                                       {:multipart-params
                                                        [["file" (-> new-file
                                                                     .-target
                                                                     .-files
                                                                     first)]]}))]
                               (if (= (:status res) 200)
                                 (on-uploaded (-> res :body :name))
                                 (js/console.log res)))))]
    (r/create-class
     {:display-name "click-upload-img"

      :component-did-mount
      (fn [this]
        (.addEventListener
         (d/dom-node this)
         "click"
         #(-> (d/dom-node this)
              (.getElementsByTagName "input")
              first
              .click)))

      :component-did-update
      (fn [this _old-argv]
        (let [_new-argv (rest (r/argv this))]))

      :reagent-render
      (fn
        [styles img _]
        [:div
         [:input {:type "file"
                  :style {:display "none"}
                  :on-change handle-changed}]
         (if (nil? img)
           [:div.has-background-grey-lighter
            {:class "modal-stage-banner-img has-text-centered"}
            [:div {:class "columns is-vcentered has-text-centered " :style {:height "100%" :margin "0px"}}
             [:div {:style {:width "100%"}}
              [:i {:class "fas fa-image fa-7x" :style {:width "100%"}}]
              [:p {:class "subtitle is-3"} "设置标题图片"]]]]
           [:figure (merge {:class "has-background-grey-lighter image modal-stage-banner-img"} styles)
            [:img {:src img :style {:height "100%" :object-fit "cover"}}]])])})))


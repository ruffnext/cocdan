(ns cocdan.core.coc.dice 
  (:require [cats.monad.either :as either]
            [clojure.string :as s]
            [cocdan.core.coc.attrs :as attrs-core]
            [cocdan.data.performer.avatar :as avatar]
            [cocdan.data.performer.core :refer [get-attr]]))

(defn- handle-r-command
  [cmd cmd-rest {avatar-id :id :as avatar}] 
  (let [attr-name (attrs-core/cover-attr-name-standard
                   (first (s/split cmd-rest #"\s+" 2)))
        attr-value (get-attr avatar attr-name)
        op-props {:avatar avatar-id :attr attr-name :attr-val attr-value}] 
    (case cmd
      "ra" (either/right ["ra" op-props])
      "rc" (either/right ["rc" op-props])
      (either/left (str "r 指令集尚未支持 " cmd " " attr-name)))))

(defn- handle-st-command
  [cmd-rest {avatar-id :id :as avatar}]
  (let [re-pattern #"(?<attr>[^0-9]+)(?<val>[0-9]+)"
        res (re-seq re-pattern (s/replace cmd-rest #"\s" ""))]
    (if-not res
      (either/left ".st 指令非法")
      (let [update-op-list (->> (map (fn [[_ key-name value]]
                                       (let [attr-standard-name (attrs-core/cover-attr-name-standard key-name)
                                             attr-standard-key (keyword (str "avatars." avatar-id ".props.attrs." attr-standard-name))]
                                         [attr-standard-key [(get-attr avatar attr-standard-name) (parse-long value)]])) res)
                                (into {})
                                (map (fn [[k v]] (vec (concat [k] v)))) vec)]
        (either/right ["update" update-op-list])))))


(defn parse-cmd
  "返回 either [op-type op-props]"
  [cmd avatar]
  (let [pure-cmd (-> (s/replace cmd #"[\r\n]" "")
                     (s/replace #"\s+" " ")
                     (s/lower-case))
        reg-pattern #"^[.。](?<cmd>(close|show|watch|sc|st|aa|r[b]+|r[p]+|rd|ra|rc|r\d{1,}d\d{1,}[^ ]*))[ ]*(?<rest>.*)"
        res (re-seq reg-pattern pure-cmd)]
    (if-not res
      (either/left "该消息并非为一个合法的 coc 指令")
      (let [[_ _ cmd cmd-rest] (first res)]
        (cond
          (s/starts-with? cmd "r") (handle-r-command cmd cmd-rest avatar)
          (= cmd "st") (handle-st-command cmd-rest avatar)
          :else (either/left (str "无法处理指令类型 " cmd))))
      )))

(comment
  (let [a (avatar/->Avatar 1 "name" "image" "description" "substage" "controlled_by" {:attrs {:str 100}})
        res (parse-cmd ".st str100   bv12" a)]
    (println res))
  
  )
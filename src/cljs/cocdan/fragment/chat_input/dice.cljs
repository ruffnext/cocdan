(ns cocdan.fragment.chat-input.dice 
  (:require [cats.monad.either :as either]
            [clojure.string :as s]
            [re-frame.core :as rf]))

(defn dice
  [{:keys [stage-id avatar-id attr]}] 
  [:button.button
   {:on-click (fn [_]
                (rf/dispatch [:play/execute-transaction-props-easy!
                              stage-id "rc" {:avatar avatar-id :attr attr}]))}
   (str "进行 " attr " 检定")])

(defn- handle-st-command
  [avatar-id cmd-rest]
  (let [re-pattern #"(?<attr>[^0-9]+)(?<val>[0-9]+)"
        res (re-seq re-pattern (s/replace (s/lower-case cmd-rest) #" " ""))]
    (if-not res
      (either/left "st 指令非法")
      (either/right (->> (map (fn [[_ key-name value]] [(keyword key-name) (parse-long value)]) res)
                         (into {})
                         ((fn [x] ["st" {:avatar avatar-id :attr-map x}])))))))

(defn- handle-rc-command
  [avatar-id cmd-rest]
  (either/right ["rc" {:avatar avatar-id :attr cmd-rest}]))

(defn parse-cmd
  [avatar-id cmd]
  (let [reg-pattern #"^[.。](?<cmd>(close|show|watch|sc|st|aa|r[b]+|r[p]+|rd|ra|rc|r\d{1,}d\d{1,}[^ ]*))[ ]*(?<rest>.*)"
        res (re-seq reg-pattern cmd)]
    (if-not res
      (either/left)
      (let [[_ _ cmd cmd-rest] (first res)]
        (case cmd
          "st" (handle-st-command avatar-id cmd-rest)
          "rc" (handle-rc-command avatar-id cmd-rest)
          (either/left (str "无法处理指令类型 " cmd)))))))

(ns cocdan.core.coc.dice
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.core :refer [parse-long]]
            [clojure.string :as s]
            [cocdan.core.coc.attrs :as attrs-core]
            [cocdan.data.performer.core :refer [get-attr]]))

(defn- handle-dice-formula
  [current-val command arg]
  (let [arg-int (parse-long arg)]
    (if arg-int
      (case command
        "+" (either/right (+ current-val arg-int))
        "-" (either/right (- current-val arg-int))
        "*" (either/right (* current-val arg-int))
        "/" (either/right (/ current-val arg-int))
        (either/left (str "无法识别计算指令" command)))
      (either/left (str "无法将 " arg " 解析为数字")))))

(defn roll-a-dice
  "投掷一次骰子，格式为 1d100 或者类似的
   返回一个 either val ，val 为骰子的点数，或者失败的原因"
  [formula]
  (let [matcher (re-seq #"^(?<dice>[1-9][0-9]*)d(?<side>[1-9][0-9]*)(?<rest>.*)" formula)
        matcher-int (re-matches #"^[1-9][0-9]*" formula)]
    (cond
      matcher-int (either/right (parse-long matcher-int))
      matcher (let [[_ dice-n side-n rest-formula] (first matcher)
                    dice-n (parse-long dice-n)
                    side-n (parse-long side-n)
                    rest-formula (re-seq #"(?<op>[\+\-\*\/])(?<val>[1-9][0-9]*)" (s/lower-case rest-formula))
                    res (apply + (for [_ (range dice-n)] (+ (rand-int side-n) 1)))]
                (if (nil? rest-formula)
                  (either/right res)
                  (m/foldm (fn [a [_ command arg]]
                             (handle-dice-formula a command arg))
                           res rest-formula)))
      (= formula "d") (roll-a-dice "1d100")
      (= formula "a") (roll-a-dice "1d100")
      (= formula "c") (roll-a-dice "1d100")
      :else (either/left (str "无法将「" formula "」解析为骰子公式")))))

(defn- new-sc-command
  [_cmd cmd-rest {avatar-id :id :as avatar}]
  (let [san (get-attr avatar "san")
        [dice-success dice-failure] (s/split cmd-rest #"/" 2)]
    (m/mlet
     [_dice-success (roll-a-dice dice-success)
      _dice-failure (roll-a-dice dice-failure)]
     (either/right
      ["sc" {:avatar avatar-id 
             :attr "san"
             :attr-val san
             :loss-on-success dice-success
             :loss-on-failure dice-failure}]))))

(defn- new-r-command
  [cmd cmd-rest {avatar-id :id :as avatar}]
  (let [attr-name (attrs-core/cover-attr-name-standard
                   (first (s/split cmd-rest #"\s+" 2)))
        attr-value (get-attr avatar attr-name)
        op-props {:avatar avatar-id :attr attr-name :attr-val attr-value}]
    (case cmd
      "ra" (either/right ["ra" op-props])
      "rc" (either/right ["rc" op-props])
      (either/left (str "r 指令集尚未支持 " cmd " " attr-name)))))

(defn- new-st-command
  [_cmd cmd-rest {avatar-id :id :as avatar}]
  (let [re-pattern #"(?<attr>[^0-9]+)(?<val>[0-9]+)"
        res (re-seq re-pattern (s/replace cmd-rest #"\s" ""))]
    (if-not res
      (either/left ".st 指令非法")
      (let [update-op-list (->> (map (fn [[_ key-name value]]
                                       (let [standard-attr-name (attrs-core/cover-attr-name-standard key-name)
                                             attr-standard-key (keyword (str "avatars." avatar-id ".payload.attrs." (name standard-attr-name)))]
                                         [attr-standard-key [(get-attr avatar standard-attr-name) (parse-long value)]])) res)
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
      (either/left (str cmd " 并非为一个合法的 coc 指令"))
      (let [[_ _ cmd cmd-rest] (first res)
            handler-fn (cond
                         (s/starts-with? cmd "r") new-r-command
                         (= cmd "st") new-st-command
                         (= cmd "sc") new-sc-command
                         :else (fn [cmd & _] (either/left (str "无法处理指令类型 " cmd))))]
        (handler-fn cmd cmd-rest avatar)))))

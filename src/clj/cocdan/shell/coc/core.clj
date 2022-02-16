(ns cocdan.shell.coc.core
  (:require
   [cats.monad.either :as either]
   [cats.core :as m]
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [cocdan.ws.db :as ws-db]
   [immutant.web.async :as async]
   [cocdan.auxiliary :as gaux]
   [cocdan.ws.auxiliary :as ws-aux]))

(defn- get-coc-attr
  [avatar col-keys]
  (let [res (reduce
             (fn [a f] (f a))
             avatar
             (-> (conj [:attributes :coc] col-keys)
                 flatten))]
    (if (nil? res)
      (either/left (format "attributes %s is nil" (str col-keys)))
      (either/right res))))

(defn- set-coc-attr
  [avatar col-keys val channel]
  (let [new-avatar (assoc-in
                    avatar
                    (-> (conj [:attributes :coc] col-keys)
                        flatten)
                    val)]
    (ws-db/upsert-db! ws-db/db :avatar new-avatar)
    (ws-db/notify-clients-db! @ws-db/db :avatar channel)
    new-avatar))

(defn- chinese-english-attr-map
  [attr-name]
  (case attr-name
    "教育" "edu"
    "灵感" "int"
    "智力" "int"
    "意志" "pow"
    "外貌" "app"
    "力量" "str"
    "敏捷" "dex"
    "体型" "siz"
    "体质" "con"
    "san值" "san"
    "理智" "san"
    "理智值" "san"
    "幸运" "luck"
    "体力" "hp"
    "运气" "luck"
    "侦查" "investigate"
    attr-name))

(defn- dice-command
  [formula]
  (let [matcher (re-matcher #"^(?<dice>[1-9][0-9]*)d(?<side>[1-9][0-9]*)(?<rest>.*)" formula)
        matcher-int (re-matcher #"^[1-9][0-9]*" formula)]
    (cond
      (.matches matcher) (let [dice-n (Integer/parseInt (.group matcher "dice"))
                               side-n (Integer/parseInt (.group matcher "side"))
                               rest-formula (re-seq #"(?<op>[\+\-\*\/])(?<val>[1-9][0-9]*)" (str/lower-case (.group matcher "rest")))
                               res (apply + (for [_ (range dice-n)] (+ (rand-int side-n) 1)))]
                           (if (nil? rest-formula)
                             (either/right res)
                             (m/foldm (fn [a [_match op val]]
                                        (either/try-either
                                         (->
                                          (str "( " (str/join " " [op a val]) " )")
                                          read-string
                                          eval
                                          int)))
                                      res
                                      rest-formula)))
      (= formula "d") (dice-command "1d100")
      (= formula "a") (dice-command "1d100")
      (= formula "c") (dice-command "1d100")
      (.matches matcher-int) (either/right (Integer/parseInt formula))
      :else (either/left "parsed failed"))))

(defn- ra
  [avatar attr-name rolled-value attr-value]
  (let [command-res (cond
                      (and (<= rolled-value attr-value) (<= rolled-value 5)) "大成功"
                      (<= rolled-value (quot attr-value 5)) "极难成功"
                      (<= rolled-value (quot attr-value 2)) "困难成功"
                      (<= rolled-value attr-value) "成功"
                      (and (> rolled-value attr-value) (> rolled-value 95)) "大失败"
                      :else "失败")]
   (str (:name avatar) "进行" attr-name "检定：\nD100=" rolled-value "/" attr-value "，" command-res)))

(defn- r
  [avatar cmd cmd-rest _channel]
  (log/debug cmd)
  (m/mlet
   [rolled-value (dice-command (subs cmd 1))]
   (cond
     (.matches (re-matcher #"^(?<dice>[1-9][0-9]*)d(?<side>[1-9][0-9]*).*" (subs cmd 1))) (either/right rolled-value)
     (= "rd" cmd) (either/right rolled-value)
     (= "rc" cmd) (m/mlet
                   [attr-val (get-coc-attr avatar [:attrs (keyword (chinese-english-attr-map cmd-rest))])]
                   (either/right (ra avatar cmd-rest rolled-value attr-val)))
     (= "ra" cmd) (m/mlet
                   [attr-val (get-coc-attr avatar [:attrs (keyword (chinese-english-attr-map cmd-rest))])]
                   (either/right (ra avatar cmd-rest rolled-value attr-val)))
     :else (either/left "this command has not been implemented yet"))))

(comment
  (log/debug (dice-command "3d6*5"))
  (log/debug (dice-command "3d6*5+1"))
  (log/debug (dice-command "3d6*5+1 理智"))
  (log/debug (dice-command "1d100"))
  (log/debug (dice-command "d"))
  (log/debug (dice-command "a"))
  (log/debug (dice-command "1x3"))
  (.matches (re-matcher #"^[1-9][0-9]*" "12.2"))
  (read-string "(+ 1 2)")
  (int (Float/parseFloat "2.33"))
  (str/join " " ["Hello" "World"])
  (.matches (re-matcher #"^(?<dice>[1-9][0-9]*)d(?<side>[1-9][0-9]*).*" (subs "r3d6*5" 1)))
  (int 1.5)
  (-> "(+ 4 1)"
      read-string
      eval))

(defn- sc
  [avatar _cmd cmd-rest channel]
  (m/mlet [san (get-coc-attr avatar [:attrs :san])
           [on-success on-failed] (let [res (str/split cmd-rest #"/")]
                                    (if (= (count res) 2)
                                      (m/foldm (fn [a x]
                                                 (m/mlet [res (dice-command x)]
                                                         (either/right (conj a res))))
                                               []
                                               (take 2 res))
                                      (either/left "sc [success]/[failed]")))
           rolled-value (dice-command "1d100")
           [res-str san-drop] (either/right (if (> rolled-value san)
                                              ["失败" on-failed]
                                              ["成功" on-success]))
           res-str (either/right
                    (str
                     (:name avatar)
                     "进行 San Check 检定：\nD100="
                     rolled-value "/" san "，" res-str
                     "\n损失理智 " san-drop " 点，现剩余理智 "
                     (- san san-drop)))
           _new-avatar (either/right (set-coc-attr avatar [:attrs :san] (- san san-drop) channel))]
           (either/right res-str)))


(comment
  (log/debug (let [res (str/split "1/1x6" #"/")]
               (if (= (count res) 2)
                 (m/foldm (fn [a x]
                            (m/mlet [res (dice-command x)]
                                    (either/right (conj a res))))
                          []
                          (take 2 res))
                 (either/left "sc [success]/[failed]")))))

(defn- set-attr
  [avatar _cmd cmd-rest channel]
  (m/mlet [res (let [res (re-seq #"(?<attr>[^0-9]+)(?<val>[0-9]+)" (str/lower-case cmd-rest))]
                 (if (nil? res)
                   (either/left "设置命令非法")
                   (either/right res)))
           new-avatar (m/foldm
                       (fn [a [_match attr-name attr-value]]
                         (m/mlet [int-v (either/try-either (Integer/parseInt attr-value))]
                                 (m/return
                                  (assoc-in
                                   a
                                   [:attributes :coc :attrs (keyword (chinese-english-attr-map attr-name))]
                                   int-v))))
                       avatar
                       res)]
          (ws-db/upsert-db! ws-db/db :avatar new-avatar)
          (ws-db/notify-clients-db! @ws-db/db :avatar channel)
          (m/return new-avatar))
  (either/right "set attr"))

(defn- handle-item-loc
  [loc item-list skill-investigate]
  (let [shown (vec (filter #(= "显露" (:hidden? %)) item-list))
        shade (vec (filter #(and (= "遮蔽" (:hidden? %)) (<= (+ (rand-int 100) 1) skill-investigate)) item-list))
        _hidden (vec (filter #(= "隐藏" (:hidden? %)) item-list))
        can-see (apply conj shown shade)]
    (if (empty? can-see)
      ""
      (format "%s有%s" loc (str/join ", " (map #(:name %) can-see))))))

(defn- watch
  [avatar _cmd cmd-rest channel]
  (m/mlet [avatars (ws-db/pull-avatars-by-channel @ws-db/db channel)
           avatar-investigate-skill (let [res (-> avatar :attributes :coc :attrs :investigate)]
                                     (if (pos-int? res)
                                       (either/right res)
                                       (either/left "你的角色没有侦查技能！")))
           target-avatar (let [res (take 1 (filter #(= (-> % :name) cmd-rest) avatars))]
                           (if (seq res)
                             (either/right (first res))
                             (either/left (format "avatar %s is not on this stage" cmd-rest))))
           _check (m/do-let
                   (if (not= (-> avatar :attributes :substage) (-> target-avatar :attributes :substage))
                     (either/left (format "avatar %s is not on this substage" cmd-rest))
                     (either/right "")))]
          (let [items (-> target-avatar :attributes :coc :items)]
            (either/right (->> (for [[k vs] items]
                                 (handle-item-loc (name k) vs avatar-investigate-skill))
                               (filter #(not (str/blank? %)))
                               (str/join "; "))))))

(defn- show-attr
  [{name :name :as avatar} _cmd cmd-rest _channel]
  (m/mlet [attr-val (let [res' (-> avatar :attributes :coc :attrs)
                          res ((keyword (chinese-english-attr-map cmd-rest)) res')]
                      (if (pos-int? res)
                        (either/right res)
                        (either/left (format "你的角色没有 %s 属性" cmd-rest))))]
          (either/right (format "%s的%s是%s" name cmd-rest (str attr-val)))))

(defn coc
  [{msg :msg
    msg-type :type
    avatar-id :avatar
    substage :substage  :as msg-raw}  channel]
  (when (= msg-type "msg")
    (let [match-res (re-matcher #"^[.。](?<cmd>(show|watch|sc|st|aa|r[b]+|r[p]+|rd|ra|rc|r\d{1,}d\d{1,}[^ ]*))[ ]*(?<rest>.*)" msg)]
      (when (.matches match-res)
        (let [res (m/mlet [[cmd cmd-rest] (either/right [(.group match-res "cmd") (.group match-res "rest")])
                           avatar (ws-db/pull-avatar-by-id @ws-db/db avatar-id)
                           res (cond
                                 (= cmd "sc") (sc avatar cmd cmd-rest channel)
                                 (= cmd "st") (set-attr avatar cmd cmd-rest channel)
                                 (str/starts-with? cmd "r") (r avatar cmd cmd-rest channel)
                                 (= cmd "watch") (watch avatar cmd cmd-rest channel)
                                 (= cmd "show") (show-attr avatar cmd cmd-rest channel)
                                 :else (either/left "command not supported"))]
                          (either/right res))]
          (either/branch res
                         (fn [left-value]
                           (let [resp-msg (assoc (ws-aux/make-msg 0 "system-msg" left-value)
                                                 :receiver avatar-id
                                                 :substage substage)]
                             (async/send! channel (gaux/->json resp-msg))))
                         (fn [right-value]
                           (let [resp-msg (assoc (ws-aux/make-msg 0 "system-msg" right-value)
                                                 :substage substage)]
                             (doseq [channel (ws-db/query-channels-by-channel @ws-db/db channel)]
                               (async/send! channel (gaux/->json resp-msg))))))))))
  (either/right msg-raw))

(comment
  (let [res (re-matcher #"^[.。](?<cmd>(sc|rb|rbb|rp|rpp|rd|ra|r\d{1,}d\d{1,}))[ ]*(?<rest>.*)" ".rd s")]
    (.matches res)
    (.group res "cmd"))

  (let [res (re-seq #"(?<attr>[^0-9]+)(?<val>[0-9]+)" "Nihao")]
    res))


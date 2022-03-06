(ns cocdan.core.coc
  (:require
   [cljs.core :as cc]
   [cats.monad.either :as either]
   [cats.core :as m]
   [datascript.core :as d]
   [cocdan.core.avatar :refer [get-avatar-attr set-avatar-attr complete-avatar-attributes set-avatar-default-attr remove-avatar-attr]]
   [cocdan.db :refer [db]]
   [clojure.string :as str]
   [reagent.core :as r]
   [cocdan.auxiliary :refer [remove-db-perfix]]
   [clojure.core.async :refer [go <!]]
   [cljs-http.client :as http]
   [posh.reagent :as p]
   [re-frame.core :as rf]))

(defn- validate-general-attributes
  [{{{val :attrs} :coc} :attributes}]
  (either/right val))

(rf/reg-event-fx
 :coc-event/refresh-occupations
 (fn [_ _]
   (go (let [res (<! (http/get "/api/files/res/docs%2Fcoc%2Foccupations.csv"))]
         (when (= (:status res) 200)
           (rf/dispatch [:rpevent/upsert :coc-occupation (:body res)]))))
   {}))

(rf/reg-event-fx
 :coc-event/refresh-skills
 (fn [_ _]
   (go (let [res (<! (http/get "/api/files/res/docs%2Fcoc%2Fskills.csv"))]
         (when (= (:status res) 200)
           (rf/dispatch [:rpevent/upsert :coc-skill (map (fn [x] (assoc x :initial (js/parseInt (:initial x)))) (:body res))]))))
   {}))


(defn posh-occupation-name-list
  []
  (p/q '[:find [?occupation-name ...]
         :where [?eid :coc-occupation/occupation-name ?occupation-name]]
       db))

(defn posh-skill-name-list
  []
  (p/q '[:find [?skill-name ...]
         :where [?eid :coc-skill/skill-name ?skill-name]]
       db))


(defn- translate-skill-name-to-eng
  [skill-name']
  (cond
    (= skill-name' "任意") "any"
    (= skill-name' "任一") "any"
    :else
    (let [skill-name (str/replace skill-name' #"。" "")]
      (or (d/q '[:find ?skill-name .
                 :in $ ?ch-name
                 :where
                 [?eid :coc-skill/skill-name-ch ?ch-name]
                 [?eid :coc-skill/skill-name ?skill-name]]
               @db
               skill-name) (str skill-name)))))

(defn translate-skill-name-to-ch
  [skill-name']
  (or (:coc-skill/skill-name-ch (d/pull @db '[:coc-skill/skill-name-ch] [:coc-skill/skill-name skill-name'])) skill-name'))


(comment
  (translate-skill-name-to-ch "credit-rating"))

(defn query-skill-by-name
  [skill-name]
  (let [eng-name (translate-skill-name-to-eng skill-name)
        eid (d/entid @db [:coc-skill/skill-name eng-name])]
    (if eid
      (remove-db-perfix (d/pull @db '[*] eid))
      {:skill-name skill-name})))

(comment
  (translate-skill-name-to-eng "射击")
  (query-skill-by-name "射击"))

(defn- chinese-n-to-int
  [chinese-n]
  (case chinese-n
    "一" 1
    "两" 2
    "三" 3
    "四" 4
    5))

(defn- handle-skill-item
  [skill-name]
  (cond
    (str/includes? skill-name "个人或时代特长")
    (let [n (chinese-n-to-int (nth skill-name 2))]
      (list ["any"] n))

    (str/includes? skill-name "社交技能")
    (let [n (chinese-n-to-int (nth skill-name 0))]
      (list (vec (map translate-skill-name-to-eng ["魅惑" "话术" "说服" "恐吓"])) n))

    (str/includes? skill-name "或")
    (list (vec (map handle-skill-item (str/split skill-name #"或"))) 1)

    :else (let [skills (str/split skill-name #"[（）]")]
            (map translate-skill-name-to-eng skills))))

(comment
  (handle-skill-item "科学（动物学）")
  (str/split "科学（动物学）" #"[（）]")
  (handle-skill-item "任意两项其他个人或时代特长。")
  (handle-skill-item "两项社交技能")
  (nth "任意两项其他个人或时代特长。" 2)
  (repeat  3 '("any")))

(defn get-coc-attr
  [avatar key-col]
  (get-avatar-attr avatar [:coc :attrs key-col]))

(defn remove-coc-attr
  [avatar key-col]
  (remove-avatar-attr avatar [:coc :attrs key-col]))

(defn set-coc-attr
  [avatar key-col value]
  (set-avatar-attr avatar [:coc :attrs key-col] value))

(defn- set-coc-default-attr
  [avatar key-col default-val]
  (set-avatar-attr avatar [:coc :attrs key-col] (or (get-coc-attr avatar key-col) default-val)))

(defn- handle-coc-formula
  [attrs f]
  (cond
    (vector? f) (let [res (reduce (fn [a x]
                                    (let [res (handle-coc-formula attrs x)]
                                      (when (and a res)
                                        (conj a res)))) [] f)]
                  (when (seq res)
                    (apply (first res) (rest res))))
    (keyword? f) (get-avatar-attr attrs (conj [:coc :attrs] f))
    :else f))

(defn- coc-attr-formula
  [attrs col-key formula]
  (let [res (handle-coc-formula attrs formula)]
    (if (nil? res)
      attrs
      (set-avatar-attr attrs (conj [:coc :attrs] col-key) res))))

(defn list-avatar-all-coc-skills
  [avatar]
  (reduce (fn [a [k _v]]
            (if (str/ends-with? (name k) "-base")
              (conj a (str/replace (name k) #"-base" ""))
              a))
          [] (get-avatar-attr avatar [:coc :attrs])))

(defn- coc-age-correction
  [avatar]
  (let [age (get-avatar-attr avatar [:age])
        [str-cor con-cor app-cor] (cond
                                    (> age 70) [-12 -12 -12]
                                    (> age 60) [-6 -6 -6]
                                    (> age 50) [-3 -3 -3]
                                    (> age 40) [-1 -1 -1]
                                    :else [0 0 0 0])]
    (-> avatar
        (set-avatar-attr [:coc :attrs :str-correction] str-cor)
        (set-avatar-attr [:coc :attrs :con-correction] con-cor)
        (set-avatar-attr [:coc :attrs :app-correction] app-cor))))

(defn- coc-pow-correction
  [avatar]
  (let [cm (get-avatar-attr avatar [:coc :attrs :cthulhu-mythos])]
    (if (pos-int? cm)
      (set-avatar-attr avatar [:coc :attrs :pow-correction] (- cm))
      avatar)))

(defn- coc-mov-correction
  [avatar]
  (let [str (get-avatar-attr avatar [:coc :attrs :str])
        dex (get-avatar-attr avatar [:coc :attrs :dex])
        siz (get-avatar-attr avatar [:coc :attrs :siz])
        age (get-avatar-attr avatar [:age])
        age-correction (if (pos-int? age)
                         (cond
                           (> age 70) -4
                           (> age 60) -3
                           (> age 50) -2
                           (> age 40) -1
                           :else 0)
                         0)
        attr-correction (cond
                          (and (> str siz) (> dex siz)) 1
                          (and (< str siz) (< dex siz)) -1
                          :else 0)]
    (set-avatar-attr avatar [:coc :attrs :mov-correction] (+ age-correction attr-correction))))

(defn- handle-skill-formula
  [avatar formula]
  (let [first-parts (str/split formula "＋")
        handle-parts (fn [parts]
                       (let [[attr multiply] (str/split parts "×")
                             attrs (->> (str/split attr "或")
                                        (map keyword)
                                        (map #(get-coc-attr avatar %))
                                        (apply max))]
                         (* (or attrs 0) (js/parseInt multiply))))]
    (apply + (map handle-parts first-parts))))

(defn calc-coc-skill-success-rate
  [avatar skill-name skill-type]
  (let [base (or (get-coc-attr avatar (keyword (str skill-name "-base"))) 0)
        occupation (if (= skill-type "occupation")
                     (or (get-coc-attr avatar (keyword (str skill-name "-occupation"))) 0)
                     0)
        interest (or (get-coc-attr avatar (keyword (str skill-name "-interest"))) 0)]
    (+ base occupation interest)))

(defn set-avatar-skill-success-rate
  [avatar skill-name skill-type]
  (set-coc-attr avatar (keyword skill-name) (calc-coc-skill-success-rate avatar skill-name skill-type)))

(defn- handle-skills-points
  [avatar {skill-formula :skill-point-formula}]
  (let [skill-points (or (handle-skill-formula avatar skill-formula) 0)]
    (set-avatar-attr avatar [:coc :attrs :occupation-skill-points] skill-points)))

(defn- handle-credit-rating-range
  [avatar {credit-rating-range' :credit-rating-range}]
  (let [credit-rating-range (if (string? credit-rating-range') (map js/parseInt (str/split credit-rating-range' #"-")) [0 100])]
    (set-avatar-attr avatar [:coc :attrs :credit-rating-range] credit-rating-range)))

(defn- handle-occupation-skills
  [avatar {occupation-skills' :occupation-skills}]
  (let [occupation-skills (vec (reduce (fn [a x]
                                         (let [res (handle-skill-item x)]
                                           (cond
                                             (vector? res) (concat a res)
                                             :else  (conj a res))))
                                       [] (if (string? occupation-skills') (str/split occupation-skills' #"[，]") [])))]
    (set-coc-attr avatar :occupation-skills (conj occupation-skills (list "credit-rating")))))

(defn clear-coc-skills-when-occupation-change
  [avatar-before avatar-now]
  (let [before-occupation (get-coc-attr avatar-before :occupation-name)
        now-occupation (get-coc-attr avatar-now :occupation-name)]
   (if (not= before-occupation now-occupation)
     (let [skills (list-avatar-all-coc-skills avatar-now)]
       (-> (reduce (fn [a x]
                     (-> (remove-coc-attr a (keyword (str x "-occupation")))
                         (remove-coc-attr (keyword (str x "-interest")))
                         (remove-coc-attr (keyword (str x "-base")))))
                   avatar-now skills)
           (remove-coc-attr :occupation-skills)
           (remove-coc-attr :interest-skills)))
     avatar-now)))

(defn- handle-occupation-related-attrs
  "skip handling when occupation does not change"
  [avatar-old avatar-now]
  (if (= (get-coc-attr avatar-old :occupation-name) (get-coc-attr avatar-now :occupation-name))
    avatar-now
    (let [occupation-name (get-coc-attr avatar-now :occupation-name)
          eid (when occupation-name
                (d/entid @db [:coc-occupation/occupation-name occupation-name]))
          skill-info (when eid
                       (remove-db-perfix (d/pull @db '[*] eid)))]
      (or (when skill-info
            (-> avatar-now
                (handle-skills-points skill-info)
                (handle-credit-rating-range skill-info)
                (handle-occupation-skills skill-info)))
          avatar-now))))

(defn list-occupation-skills-from-avatar
  [avatar]
  (let [occupation-skills (get-coc-attr avatar :occupation-skills)]
    (-> (reduce (fn [a [skill-name _ & res]]
                  (cond
                    (string? skill-name) (conj a skill-name)
                    (and (vector? skill-name) (seq res)) (concat a res)
                    :else a))
                []
                occupation-skills)
        sort)))


(defn calc-coc-used-occupation-skill-points
  [avatar]
  (let [occupation-skills (list-occupation-skills-from-avatar avatar)]
    (apply + (map #(or (get-coc-attr avatar (keyword (str % "-occupation"))) 0) occupation-skills))))

(defn calc-coc-used-interest-skill-points
  [avatar]
  (let [occupation-skills (reduce (fn [a [k _v]]
                                    (if (str/ends-with? (name k) "-base")
                                      (conj a (str/replace (name k) #"-base" ""))
                                      a))
                                  [] (get-avatar-attr avatar [:coc :attrs]))]
    (apply + (map #(or (get-coc-attr avatar (keyword (str % "-interest"))) 0) occupation-skills))))

(defn handle-initial-interest-skills
  [avatar-before avatar-now]
  (let [interest-skills (get-coc-attr avatar-now :interest-skills)]
    (if (and interest-skills (= (get-coc-attr avatar-before :occupation-name) (get-coc-attr avatar-now :occupation-name)))
      avatar-now
      (let [attrs (get-avatar-attr avatar-now [:coc :attrs])
            skills (reduce (fn [a [k _v]]
                             (if (str/ends-with? (name k) "-base")
                               (conj a (str/replace (name k) #"-base" ""))
                               a))
                           [] attrs)
            occupation-skills (set (list-occupation-skills-from-avatar avatar-now))
            interest-skills (reduce (fn [a x]
                                      (if (contains? occupation-skills x)
                                        a
                                        (conj a (list x))))
                                    []
                                    skills)
            avatar-modified (set-coc-attr avatar-now :interest-skills interest-skills)]
        (reduce (fn [a x]   ;; calc initial success rate
                  (cond
                    (set? x) a
                    :else (set-avatar-skill-success-rate a (first x) "interest")))
                avatar-modified
                interest-skills)))))

(defn remove-coc-skill
  [avatar skill-name-eng]
  (let [res (reduce (fn [a x]
                      (update-in a [:attributes :coc :attrs] dissoc (keyword x)))
                    avatar
                    [skill-name-eng
                     (str skill-name-eng "-base")
                     (str skill-name-eng "-occupation")
                     (str skill-name-eng "-interest")])]
    (handle-initial-interest-skills {} res)))

(defn avatar-add-coc-interest-skill
  [avatar skill-name-eng]
  (let [skill (remove-db-perfix (d/pull @db '[*] [:coc-skill/skill-name skill-name-eng]))
        interest-skills-raw (get-coc-attr avatar :interest-skills)
        interest-skills-set (set (map first interest-skills-raw))]
    (if (and skill (not (contains? interest-skills-set (:skill-name skill))))
      (->
       avatar
       (set-coc-default-attr (keyword (str skill-name-eng "-base")) (:initial skill))
       (set-coc-default-attr (keyword (str skill-name-eng)) (:initial skill))
       (set-coc-default-attr (keyword (str skill-name-eng "-interest")) 0)
       (#(handle-initial-interest-skills {} %)))
      avatar)))

(defn- handle-db
  [avatar-now]
  (let [str (or (get-coc-attr avatar-now :str) 0)
        siz (or (get-coc-attr avatar-now :siz) 0)
        v (+ str siz)
        db (cond
             (< v 64) "-2"
             (< v 84) "-1"
             (< v 124) "+0"
             (< v 164) "+1d4"
             (< v 204) "+1d6"
             (< v 284) "+2d6"
             (< v 364) "+3d6"
             (< v 444) "+4d6"
             :else "+0")]
    (set-coc-attr avatar-now :db db)))

(defn- handle-hp-mp-san
  [avatar-before avatar-now]
  (let [handler (fn [avatar-before avatar-now key-col]
                  (let [max-key-col (keyword (str "max-" (name key-col)))
                        before-val (get-coc-attr avatar-before key-col)
                        before-max (get-coc-attr avatar-before max-key-col)
                        now-val (get-coc-attr avatar-now key-col)
                        now-max (get-coc-attr avatar-now max-key-col)]
                    (if(or (and before-val (= before-val before-max)) (nil? now-val))
                      (set-coc-attr avatar-now key-col now-max)
                      avatar-now)))]
    (reduce (fn [a x]
              (handler avatar-before a x))
            avatar-now
            [:hp :mp :san])))

(defn- sort-coc-attr
  [avatar]
  (set-avatar-attr avatar [:coc :attrs] (into (sorted-map) (get-avatar-attr avatar [:coc :attrs]))))

(defn- coc-default-avatar
  [avatar]
  (-> avatar
      (set-avatar-default-attr [:coc :attrs :cthulhu-mythos] 0)))

(defonce test-avatar (r/atom {}))

(comment
  @test-avatar
  (list-occupation-skills-from-avatar @test-avatar)
  (get-coc-attr @test-avatar :occupation-skills)
  (calc-coc-skill-success-rate @test-avatar "dodge" "interest")
  (calc-coc-used-occupation-skill-points @test-avatar)
  (calc-coc-used-interest-skill-points @test-avatar)
  (list-avatar-all-coc-skills @test-avatar)
  (js/console.log (remove-avatar-attr @test-avatar [:coc]))
  (js/console.log (update-in @test-avatar [:attributes] dissoc :coc))
  )

(defn complete-coc-avatar-attributes
  [avatar-before avatar-now]
  (-> (complete-avatar-attributes avatar-before avatar-now)
      coc-default-avatar
      (#(clear-coc-skills-when-occupation-change avatar-before %))
      ;; attrs
      (coc-attr-formula :max-hp  [quot [+ :con :siz] 10])
      (coc-attr-formula :max-mp [quot :pow 5])
      (coc-attr-formula :max-san [- :pow :cthulhu-mythos])
      (coc-attr-formula :mov 8)
      (coc-attr-formula :interest-skill-points [* :int 2])
      coc-age-correction
      coc-pow-correction
      coc-mov-correction
      handle-db
      ;; hp, mp and etc.
      (#(handle-hp-mp-san avatar-before %))
      (set-coc-default-attr :healthy "健康")
      (set-coc-default-attr :sanity "清醒")
      ;; skills
      (coc-attr-formula :dodge-base [quot :dex 2])
      (coc-attr-formula (keyword "language(own)-base") [identity :edu])
      (#(handle-occupation-related-attrs avatar-before %))
      (#(handle-initial-interest-skills avatar-before %))
      (sort-coc-attr)))

(comment
  (let [formula "edu×2＋dex或pow×2"
        first-parts (str/split formula "＋")
        handle-parts (fn [parts]
                       (let [[attr multiply] (str/split parts "×")
                             attrs (->> (str/split attr "或")
                                        (map keyword)
                                        (map #(get-coc-attr @test-avatar %))
                                        (apply max))]
                         (* (or attrs 0) (js/parseInt multiply))
                         attrs))]
    (map handle-parts first-parts))
  (map #(get-coc-attr {:attributes :coc :attrs {:edu 10 :dex 10}} %) '(:edu))
  (get-avatar-attr @test-avatar [:coc :attrs :dex]))

(defn validate-coc-avatar
  [avatar]
  (m/->=
   (validate-general-attributes avatar)))

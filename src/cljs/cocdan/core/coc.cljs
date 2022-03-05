(ns cocdan.core.coc
  (:require
   [cljs.core :as cc]
   [cats.monad.either :as either]
   [cats.core :as m]
   [datascript.core :as d]
   [cocdan.core.avatar :refer [get-avatar-attr set-avatar-attr complete-avatar-attributes set-avatar-default-attr]]
   [cocdan.db :refer [db]]
   [clojure.string :as str]
   [reagent.core :as r]
   [cocdan.auxiliary :refer [remove-db-perfix]]
   [clojure.core.async :refer [go <!]]
   [cljs-http.client :as http]
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
(comment
  (let [a [{:a "1"} {:a "2"}]]
    (map (fn [x] (assoc x :a 1)) a))
  )


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


(defn query-skill-by-name
  [skill-name]
  (let [eng-name (translate-skill-name-to-eng skill-name)
        eid (d/entid @db [:coc-skill/skill-name eng-name])]
    (if eid
      (remove-db-perfix (d/pull @db '[*] eid))
      {:skill-name skill-name})))

(comment
  (translate-skill-name-to-eng "射击")
  (query-skill-by-name "射击")
  )

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
      (vec (repeat n (list "any"))))
    
    (str/includes? skill-name "社交技能")
    (let [n (chinese-n-to-int (nth skill-name 0))]
      (vec (repeat n (set (map translate-skill-name-to-eng ["魅惑" "话术" "说服" "恐吓"])))))
    
    (str/includes? skill-name "或")
    (set (map handle-skill-item (str/split skill-name #"或")))
    
    :else (let [skills (str/split skill-name #"[（）]")]
            (map translate-skill-name-to-eng skills))))

(comment
  (handle-skill-item "科学（动物学）")
  (str/split "科学（动物学）" #"[（）]")
  (handle-skill-item "任意两项其他个人或时代特长。")
  (handle-skill-item "两项社交技能")
  (nth "任意两项其他个人或时代特长。" 2)
  (repeat  3 '("any"))
  )

(defn get-coc-attr
  [avatar key-col]
  (get-avatar-attr avatar [:coc :attrs key-col]))

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

(defn- handle-skills-points
  [avatar {skill-formula :skill-point-formula}]
  (let [skill-points (or (handle-skill-formula avatar skill-formula) 0)]
    (set-avatar-attr avatar [:coc :attrs :interest-skill-points] skill-points)))

(defn- handle-credit-rating-range
  [avatar {credit-rating-range' :credit-rating-range}]
  (let [credit-rating-range (if (string? credit-rating-range') (map js/parseInt (str/split credit-rating-range' #"-")) [0 100])]
    (set-avatar-attr avatar [:coc :attrs :credit-rating-range] credit-rating-range)))

(defn- handle-occupation-skills
  [avatar {occupation-skills' :occupation-skills}]
  (let [occupation-skills (reduce (fn [a x]
                                    (let [res (handle-skill-item x)]
                                      (if (vector? res)
                                        (concat a res)
                                        (conj a res)))) [] (if (string? occupation-skills') (str/split occupation-skills' #"[，]") []))]
    (set-coc-attr avatar :occupation-skills occupation-skills)))

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
    (reduce (fn [a x]
              (js/console.log x)
              (cond
                (set? x) a
                (= (first x) "any") a
                (seq? x) (conj a (first x))
                :else a))
            []
            occupation-skills)))

(defn list-interest-skills
  [avatar]
  (let [occupation-skills (set (list-occupation-skills-from-avatar avatar))]
    ; TODO: filter interest skills
    ))

(defn- sort-coc-attr
  [avatar]
  (set-avatar-attr avatar [:coc :attrs] (into (sorted-map) (get-avatar-attr avatar [:coc :attrs])))
  )

(defn- coc-default-avatar
  [avatar]
  (-> avatar
      (set-avatar-default-attr [:coc :attrs :cthulhu-mythos] 0)))

(defonce test-avatar (r/atom {}))

(comment
  (list-occupation-skills-from-avatar @test-avatar)
  (get-coc-attr @test-avatar :occupation-skills)
  )

(defn complete-coc-avatar-attributes
  [avatar-before avatar-now]
  (reset! test-avatar avatar-now)
  (-> (complete-avatar-attributes avatar-before avatar-now)
      coc-default-avatar
      (coc-attr-formula :max-hp  [quot [+ :con :siz] 10])
      (coc-attr-formula :max-mp [quot :pow 5])
      (coc-attr-formula :max-san [- :pow :cthulhu-mythos])
      (coc-attr-formula :mov 8)
      coc-age-correction
      coc-pow-correction
      coc-mov-correction
      (#(set-avatar-attr % [:coc :attrs :san] (or (get-coc-attr % :san) (get-coc-attr % :max-san))))
      (#(set-avatar-attr % [:coc :attrs :hp] (or (get-coc-attr % :hp) (get-coc-attr % :max-hp))))
      (#(set-avatar-attr % [:coc :attrs :mp] (or (get-coc-attr % :mp) (get-coc-attr % :max-mp))))
      (set-coc-default-attr :healthy "健康")
      (set-coc-default-attr :sanity "清醒")
      ;; skills
      (coc-attr-formula :dodge [quot :dex 2])
      (coc-attr-formula (keyword "language(own)") [identity :edu])
      (#(handle-occupation-related-attrs avatar-before %))
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
  (get-avatar-attr @test-avatar [:coc :attrs :dex])
  )

(defn validate-coc-avatar
  [avatar]
  (m/->=
   (validate-general-attributes avatar)))

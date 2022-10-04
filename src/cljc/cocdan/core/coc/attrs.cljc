(ns cocdan.core.coc.attrs)

(def base-list
  [[:str [:zh "力量"]]
   [:dex [:zh "敏捷"]]
   [:pow [:zh "意志"]]
   [:con [:zh "体质"]]
   [:app [:zh "外貌"]]
   [:edu [:zh "教育"]]
   [:siz [:zh "体型"]]
   [:int [:zh "灵感"] [:zh "智力"]]
   [:san [:zh "理智"] [:zh "理智值"]]
   [:luck [:zh "幸运"] [:zh "运气"]]
   [:mp [:zh "魔法"]]
   [:hp [:zh "体力"]]
   [:san [:zh "san值"]]
   [:investigate [:zh "侦查"]]])

(def standard-to-localization
  (->> (map (fn [[standard-key & localizations]]
              (map (fn [[lang-key value]]
                     [(keyword (str (name standard-key) "." (name lang-key))) value]) localizations))
            base-list)
       (apply concat)
       (into {})))

(def localization-to-standard
  (->> (map (fn [[standard-key & localizations]]
              (map (fn [[_lang-key value]] [(keyword value) standard-key]) localizations))
            base-list)
       (apply concat)
       (into {})))

(defn cover-attr-name-standard
  [attr-name]
  (name (or ((keyword attr-name) localization-to-standard) attr-name)))

(defn get-attr-localization-name
  [standard-attr-name localization-key]
  (let [find-key (keyword (str standard-attr-name "." (name localization-key)))]
    (or (find-key standard-to-localization) standard-attr-name)))

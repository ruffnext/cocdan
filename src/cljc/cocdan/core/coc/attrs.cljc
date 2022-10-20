(ns cocdan.core.coc.attrs
  (:require [tongue.core :as tongue]))

(def i11n-dict
  {:zh {:str "力量"
        :dex "敏捷"
        :pow "意志"
        :con "体质"
        :app "外貌"
        :edu "教育"
        :siz "体型"
        :int "智力"
        :san "理智"
        :luck "幸运"
        :mp "魔法值"
        :hp "体力值"
        :investigate "侦查"
        :tongue/missing-key "{1}"}})


(def tr (tongue/build-translate i11n-dict))

(def standard-to-localization
  (->> (map (fn [[_k v]] (vec v)) i11n-dict)
       (apply concat)
       (filter (fn [[k _v]] (not= (namespace k) "tongue")))
       (map (fn [[k v]]
              [(keyword v) k]))
       (into {})))

(defn cover-attr-name-standard
  [attr-name]
  (name (or ((keyword attr-name) standard-to-localization) attr-name)))

(defn get-attr-localization-name
  [standard-attr-key localization-key]
  (tr localization-key standard-attr-key))

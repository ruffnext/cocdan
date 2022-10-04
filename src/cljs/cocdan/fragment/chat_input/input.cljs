(ns cocdan.fragment.chat-input.input
  (:require ["antd" :refer [Mentions]]
            [cocdan.core.settings :as settings]
            [cocdan.data.mixin.territorial :refer [get-substage-id]]
            [cocdan.fragment.chat-input.dice :refer [parse-cmd]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [cats.monad.either :as either]))

(defn input
  [{:keys [avatar-id substage-id stage-id avatars]}]
  (r/with-let
    [input-key (r/atom (js/Math.random))]
    (let [all-avatars (map second avatars)
          is-kp (settings/query-setting-value-by-key :is-kp)
          same-substage-avatars (filter #(or (= (get-substage-id %) substage-id) (and is-kp (= 0 (:id %)))) all-avatars)
          mentionable-avatars (filter #(not= (:id %) avatar-id) same-substage-avatars)
          on-textarea-enter (fn [x]
                              (let [value (-> x .-target .-value)]
                                (either/branch
                                 (parse-cmd avatar-id value)
                                 (fn [_left]
                                   (rf/dispatch [:play/execute-transaction-props-easy! stage-id "speak" {:substage substage-id :avatar avatar-id :message value :props {}}]))
                                 (fn [right]
                                   (rf/dispatch (vec (concat [:play/execute-transaction-props-easy! stage-id] right)))))
                                (reset! input-key (js/Math.random))))]
      (with-meta
        [:> Mentions
         {:autoSize true
          :spellCheck false
          :disabled (if (some? avatar-id) false true)
          :onPressEnter on-textarea-enter}
         (doall
          (for [a mentionable-avatars]
            (with-meta [:> (.-Option Mentions) {:value (str (:id a))} (:name a)] {:key (:id a)})))]
        {:key @input-key}))))

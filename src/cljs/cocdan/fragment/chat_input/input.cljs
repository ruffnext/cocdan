(ns cocdan.fragment.chat-input.input
  (:require ["antd" :refer [Mentions]]
            [cats.monad.either :as either]
            [cocdan.core.coc.dice :refer [parse-cmd]]
            [cocdan.core.settings :as settings]
            [cocdan.data.mixin.territorial :refer [get-substage-id]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn input
  [{:keys [avatar-id substage-id stage-id avatars speak-type]}]
  (r/with-let
    [input-key (r/atom (js/Math.random))]
    (let [all-avatars (map second avatars)
          is-kp (settings/query-setting-value-by-key :game-play/is-kp)
          same-substage-avatars (filter #(or (= (get-substage-id %) substage-id) (and is-kp (= 0 (:id %)))) all-avatars)
          mentionable-avatars (filter #(not= (:id %) avatar-id) same-substage-avatars)
          on-textarea-enter (fn [x]
                              (let [value (-> x .-target .-value)]
                                (either/branch
                                 (parse-cmd value ((keyword (str avatar-id)) avatars))
                                 (fn [_left]
                                   (rf/dispatch [:play/execute-transaction-props-to-remote-easy! stage-id speak-type {:substage substage-id :avatar avatar-id :message value :props {}}]))
                                 (fn [right]
                                   (rf/dispatch (vec (concat [:play/execute-transaction-props-to-remote-easy! stage-id] right)))))
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

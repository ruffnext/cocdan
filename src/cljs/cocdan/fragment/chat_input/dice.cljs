(ns cocdan.fragment.chat-input.dice 
  (:require [re-frame.core :as rf]
            [cocdan.core.coc.attrs :as attrs-core]
            [cocdan.core.settings :as settings]))

(defn dice
  [{:keys [stage-id avatar-id attr]}] 
  (let [attr-standard-name (attrs-core/cover-attr-name-standard attr)
        attr-zh-name (attrs-core/get-attr-localization-name attr (settings/query-setting-value-by-key :ui/language))]
    [:button.button
     {:on-click (fn [_]
                  (rf/dispatch [:play/execute-transaction-props-to-remote-easy!
                                stage-id "rc" {:avatar avatar-id :attr attr-standard-name}]))}
     (str "进行 " attr-zh-name " 检定")]))

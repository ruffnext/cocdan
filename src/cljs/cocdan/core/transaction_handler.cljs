(ns cocdan.core.transaction-handler
  (:require [cats.monad.either :as either]
            [cocdan.data.core :as data-core]
            [cocdan.data.stage :refer [new-stage]]
            [cocdan.data.transaction.dice :as t-dice]
            [cocdan.data.transaction.patch :as patch]
            [cocdan.database.ctx-db.core :as ctx-db]
            [re-frame.core :as rf]
            [re-frame.db :refer [app-db]]))

(defn handle-dice-transaction
  [{{:keys [avatar attr attr-val dice-result]} :payload type :type} {_ctx-payload :payload}]
  (case type
    "rc" (either/right (t-dice/->RC avatar attr attr-val dice-result))
    "ra" (either/right (t-dice/->RA avatar attr attr-val dice-result))
    (either/left (str "无法处理骰子类型为 " type " 的骰子"))))

(defn handle-update-transaction
  [{:keys [payload id]} {{stage-id :id} :payload}]
  (let [{:keys [avatar-id substage-id]} (get-in @app-db [:play])
        substage-changes (->> (map (fn [[a b c]]
                                     (let [re-result (re-matches #"avatars\.(?<op>[\d])\.substage" (name a))]
                                       (when re-result [(parse-long (second re-result)) b c]))) payload)
                              (filter some?))] 
    (when substage-id
      (when-let [[_ _ substage-after] (first (filter (fn [[this-avatar-id substage-before _]]
                                                       (and (= avatar-id this-avatar-id)
                                                            (= substage-id substage-before))) substage-changes))]
        (let [last-transaction-id (ctx-db/query-ds-latest-transaction-id @(ctx-db/query-stage-db stage-id))] 
          (when (and substage-after (= (inc last-transaction-id) id)) ;; 当 kp 改变玩家操控角色的子舞台时，强制玩家的子舞台跟着切换
            (rf/dispatch [:play/change-substage-id! substage-after])))))
    (either/right (patch/->TPatch payload))))

(defn handle-update-context
  [{:keys [payload]} {ctx :payload}] 
  (either/right (new-stage (data-core/update' ctx (:ops payload)))))

(defn handle-snapshot-context
  [{:keys [payload]} _ctx] (either/right (new-stage payload)))

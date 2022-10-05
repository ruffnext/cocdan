(ns cocdan.services.dice.core
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [clojure.tools.logging :as log]
            [cocdan.core.coc.dice :as dice]
            [cocdan.core.ops.core :refer [register-context-handler
                                          register-transaction-handler]]
            [cocdan.data.performer.core :refer [get-attr]]
            [cocdan.data.transaction.dice :refer [handle-sc-context
                                                  handle-st-context]]))

(defn handle-attr-check-dice
  [{ctx :context/props} {{:keys [avatar attr]} :props}]
  (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])
        attr-val (get-attr avatar-record attr)] 
    (either/branch-right
     (dice/roll-a-dice "1d100")
     (fn [dice-result]
       (either/right  {:avatar avatar :attr attr :attr-val attr-val :dice-result dice-result})))))

(defn handle-sc-transaction
  [{ctx :context/props} {{:keys [avatar loss-on-success loss-on-failure] :as props} :props}]
  (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])
        san-val (get-attr avatar-record "san")]
    (m/mlet
     [dice-result (dice/roll-a-dice "1d100")
      loss-result (if (<= dice-result san-val)
                    (dice/roll-a-dice loss-on-success)
                    (dice/roll-a-dice loss-on-failure))]
     (either/right
      (assoc props :dice-result dice-result :attr-val san-val :san-loss loss-result)))))

(register-transaction-handler :rc handle-attr-check-dice)
(register-transaction-handler :ra handle-attr-check-dice)
(register-context-handler :st handle-st-context)
(register-context-handler :sc handle-sc-context)
(register-transaction-handler :sc handle-sc-transaction)

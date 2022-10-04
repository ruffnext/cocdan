(ns cocdan.services.dice.core
  (:require [clojure.tools.logging :as log]
            [cocdan.core.ops.core :refer [register-context-handler
                                          register-transaction-handler]]
            [cocdan.data.performer.core :refer [get-attr]]
            [cocdan.data.transaction.dice :refer [handle-st-context]]))

(defn handle-dice
  [{ctx :context/props} {{:keys [avatar attr]} :props}]
  (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])
        attr-val (get-attr avatar-record attr)] 
    {:avatar avatar :attr attr :attr-val attr-val :dice-result (inc (rand-int 100))}))

(register-transaction-handler :rc handle-dice)
(register-transaction-handler :ra handle-dice)
(register-context-handler :st handle-st-context)

(ns cocdan.services.journal.handler 
  (:require [cats.monad.either :as either]
            [cocdan.core.ops.core :refer [register-context-handler]]
            [cocdan.data.core :as data-core]
            [cocdan.data.stage :refer [new-stage]]))

(defn handle-update-context
  [{:keys [payload]} {ctx-payload :payload}] 
  (either/right (new-stage (data-core/update' ctx-payload payload))))

(defn handle-snapshot-context
  [{:keys [payload]} _ctx] (either/right (new-stage payload)))

(register-context-handler :snapshot handle-snapshot-context)  ; refined
(register-context-handler :update handle-update-context)      ; refined

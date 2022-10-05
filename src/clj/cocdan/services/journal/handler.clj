(ns cocdan.services.journal.handler 
  (:require [cats.monad.either :as either]
            [cocdan.core.ops.core :refer [register-context-handler]]
            [cocdan.data.core :as data-core]
            [cocdan.data.stage :refer [new-stage]]))

(defn handle-update-context
  [{ctx :context/props} {:keys [props]}] (either/right (new-stage (data-core/update' ctx props))))

(defn handle-snapshot-context
  [_ctx {:keys [props]}] (either/right (new-stage props)))

(register-context-handler :snapshot handle-snapshot-context)
(register-context-handler :update handle-update-context)

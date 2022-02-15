(ns cocdan.pages.modals
  (:require
   [cocdan.modals.stage-find :refer [stage-find]]
   [cocdan.modals.stage-info :refer [stage-info]]
   [cocdan.modals.stage-edit :refer [stage-edit]]
   [cocdan.modals.stage-delete :refer [stage-delete]]
   [cocdan.modals.general-attr-editor :refer [general-attr-editor]]
   [cocdan.modals.substage-edit :refer [substage-edit]]
   [cocdan.components.coc.avatar-edit :refer [coc-avatar-edit]]))


(defn page
  []
  [:div#modals-container
   (stage-find)
   (stage-info)
   (stage-edit)
   (stage-delete)
   (general-attr-editor)
   (substage-edit)
   (coc-avatar-edit)])
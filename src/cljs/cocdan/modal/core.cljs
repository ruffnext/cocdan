(ns cocdan.modal.core
  (:require [cocdan.modal.substage-edit :as substage-edit]
            [cocdan.modal.avatar-edit :as avatar-edit]))

(defn page
  []
  [:div#modal-container
   (substage-edit/modal)
   (avatar-edit/modal)])
(ns cocdan.schema)

(def Avatar
  {:id int?
   :name string?
   :image string?
   :description string?
   :stage int?
   :substage string?
   :controlled_by int?
   :props associative?})

(def Stage
  {:id int?
   :name string?
   :introduction string?
   :image string?
   :substages [associative?]
   :avatars [int?]
   :controlled_by int?})

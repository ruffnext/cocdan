(ns cocdan.schema)

(def Avatar
  {:id int?
   :name string?
   :image string?
   :description string?
   :stage int?
   :substage string?
   :controlled_by int?
   :payload associative?})

(def SubStage
  {:id string?
   :name string?
   :adjacencies [string?]
   :payload associative?})

(def Stage
  {:id int?
   :name string?
   :introduction string?
   :image string?
   :substages {:substage-id SubStage}
   :avatars {:avatar-id Avatar}
   :controlled_by int?})

(def StageNew
  {:name string?
   :introduction string?
   :image string?
   :substages associative?})

(def Speak
  {:message string? 
   :payload associative?})

(def Transact
  {:type string?
   :payload any?})

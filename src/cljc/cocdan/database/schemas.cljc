(ns cocdan.database.schemas)

(def main-database-schema
  {:setting/key {:db/unique :db.unique/identity}
   :avatar/id {:db/unique :db.unique/identity}
   :stage/id {:db/unique :db.unique/identity}
   :user/id {:db/unique :db.unique/identity}})

(def play-room-database-schema
  {:play/id {:db/unique :db.unique/identity}
   :transact/id {:db/unique :db.unique/identity}
   :context/id {:db/unique :db.unique/identity}})
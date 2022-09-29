-- :name create-user! :insert :1
-- :doc creates a new user record
INSERT INTO users
(username, nickname)
VALUES (:username, :nickname)

-- :name get-user-by-id :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name get-user-by-username :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE username = :username

-- :name create-stage! :insert :1
-- :doc 创建一个舞台
INSERT INTO stages
(name, introduction, image, substages, avatars, controlled_by)
VALUES (:name, :introduction, :image, :substages, :avatars, :controlled_by)

-- :name create-avatar! :insert :1
-- :doc 创建角色
INSERT INTO avatars
(name, image, description, stage, substage, controlled_by, props)
VALUES (:name, :image, :description, :stage, :substage, :controlled_by, :props)

-- :name get-transaction :? :1
-- :doc 获得 transaction
SELECT * FROM transactions
WHERE id = :id AND stage = :stage

-- :name insert-transaction! :insert :1
-- :doc 创建一个 transaction
INSERT INTO transactions
(id, stage, type, props)
VALUES (:id, :stage, :type :props)

-- :name get-avatars-by-user-id :? :*
-- :doc 获得用户所有的 avatars
SELECT * FROM avatars
WHERE controlled_by = :id

-- :name get-avatars-by-stage-id :? :*
-- :doc 获得参加舞台的所有 avatars
SELECT * FROM avatars
WHERE stage = :id

-- :name general-delete :! :n
-- :doc 简单的删除器
DELETE FROM :i:table
WHERE id = :id

-- :name general-get-by-id :? :1
-- :doc 通过 id 获得记录
SELECT * FROM :i:table
WHERE id = :id

-- :name general-updator :! :n
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
UPDATE :i:table SET
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
      " = :v:updates." (name field))))
~*/
where id = :id

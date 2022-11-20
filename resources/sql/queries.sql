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
(name, image, description, stage, substage, controlled_by, payload)
VALUES (:name, :image, :description, :stage, :substage, :controlled_by, :payload)

-- :name get-transaction :? :1
-- :doc 获得 transaction
SELECT * FROM transactions
WHERE id = :id AND stage = :stage

-- :name insert-transaction! :insert :1
-- :doc 创建一个 transaction
INSERT INTO transactions
(id, ctx_id, user, stage, time, type, payload)
VALUES (:id, :ctx_id, :user, :stage, :time, :type, :payload)

-- :name insert-context! :insert :1
-- :doc 创建一个 context
INSERT INTO contexts
(id, stage, time, payload)
VALUES (:id, :stage, :time, :payload)

-- :name list-transactions-recent :? :*
-- :doc 获得最近的 limit 个 transaction
SELECT * FROM transactions
WHERE stage = :stage-id
ORDER BY id DESC LIMIT :limit

-- :name list-transactions-after-n :? :*
-- :doc 获得从 n 之后的 transaction （包含）
SELECT * FROM transactions
WHERE stage = :stage-id AND id >= :n
ORDER BY id DESC LIMIT :limit

-- :name list-transactions-desc :? :*
-- :doc 倒序获得 transaction
SELECT * FROM transactions
WHERE stage = :stage AND id > :begin
ORDER BY id DESC LIMIT :limit OFFSET :offset

-- :name list-transactions :? :*
-- :doc 顺序获得 transaction
SELECT * FROM transactions
WHERE stage = :stage AND id > :begin
ORDER BY id LIMIT :limit OFFSET :offset

-- :name get-stage-latest-context-id :? :1
-- :doc 获取上一个 context 的 id
SELECT max(id) FROM contexts
WHERE stage = :stage-id 

-- :name get-stage-latest-context :? :1
-- :doc 获得舞台最新的 context
SELECT * FROM contexts
WHERE stage = :stage-id AND id = (SELECT max(id) FROM contexts WHERE stage = :stage-id)

-- :name get-stage-context-by-id :? :1
-- :doc 获得舞台的 context 
SELECT * FROM contexts
WHERE stage = :stage-id AND id = :id

-- :name get-stage-latest-transaction-id :? :1
-- :doc 获取最新的 transaction 的 id
SELECT max(id) FROM transactions
WHERE stage = :stage-id

-- :name get-stage-latest-transaction :? :1
-- :doc 获取舞台最新的 transaction
SELECT * FROM transactions
WHERE stage = :stage-id AND id = (SELECT max(id) FROM transactions WHERE stage = :stage-id)

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

-- :name general-updater :! :n
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

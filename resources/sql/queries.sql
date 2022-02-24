-- :name save-message! :! :n
-- :doc create a new message
INSERT INTO cocdan
(name, message, timestamp)
VALUES
(:name, :message, :timestamp)

-- :name get-messages :? :*
-- :doc selects all available message
SELECT * from cocdan

-- Commands
-- `!` --> execute (statement like insert)
-- `?` --> fetch (query)

-- Type of Results
-- `*` --> vectors
-- `n` --> number of rows affected (inserted/updated/deleted)
-- `1` --> one row
-- `raw` > passthrough an untouched result (default)


-- :name registered? :? :*
-- :doc 
SELECT * FROM users WHERE email = :email

-- :name register-user! :insert :n
-- :doc register a new user
INSERT INTO users
(name, email) 
VALUES
(:name, :email)

-- :name get-user-by-email? :? :*
-- :doc get a user by email
SELECT * FROM users WHERE email = :email

-- :name get-user-by-id? :? :*
-- :doc get a user by id
SELECT * FROM users WHERE id = :id



-- STAGE --

-- :name create-avatar! :insert :1
-- :doc create a new avatar
INSERT INTO avatars
(name, controlled_by, on_stage)
VALUES
(:name, :controlled_by, :on_stage);
--;;

-- :name list-avatars-by-user? :? :*
-- :doc list all avatars controlled by someone
SELECT * FROM avatars WHERE controlled_by = :controlled_by

-- :name list-avatars-by-stage? :? :*
-- :doc list all avatars on specified stage
SELECT * FROM avatars WHERE on_stage = :on_stage

-- :name list-avatars-by-stage? :? :*
-- :doc list all avatars on specified stage
SELECT * FROM avatars WHERE on_stage = :on_stage

-- :name get-avatar-by-id? :? :*
-- :doc get avatar by id
SELECT * FROM avatars WHERE id = :id

-- :name delete-avatar-by-id! :! :n
-- :doc delete an avatar by id
DELETE FROM avatars WHERE id = :id

-- :name transfer-avatar! :! :n
-- :doc transfer avatar's control to another user by id
UPDATE avatars SET controlled_by = :controlled_by WHERE id = :id

-- :name general-transfer :! :n
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
update :i:table set
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
      " = :v:updates." (name field))))
~*/
where id = :id



-- STAGE --

-- :name create-stage! :insert :1
-- :doc create a stage
INSERT INTO stages
(title, owned_by, banner, introduction, code)
VALUES
(:title, :owned_by, :banner, :introduction, :code)


-- :name find-stage-by-name? :? :*
-- :doc find stage by name
SELECT * FROM stages WHERE name = :name

-- :name get-stage-by-id? :? :1
-- :doc get stage by id
SELECT * FROM stages WHERE id = :id

-- :name delete-stage! :! :n
-- :doc delete a stage by id
DELETE FROM stages WHERE id = :id


-- ACTION --

-- :name list-actions-by-stage-id? :? :*
-- :doc list all actions by stage id
SELECT * FROM stage_action WHERE stage = :stage ORDER BY `order`

-- :name get-action-by-stage-id-and-order :? :*
SELECT * FROM stage_action WHERE `stage` = :stage AND `order` = :order

-- :name insert-action! :! :n
-- :doc insert an action
INSERT INTO stage_action 
(`order`, `type`, `fact`, `time`, `stage`)
VALUES
(:order, :type, :fact, :time, :stage)

-- :name update-action!! :! :n
-- :doc update an action! use it carefully!
UPDATE stage_action SET `fact` = :fact WHERE stage = :stage AND `order` = :order

-- :name clear-history-actions! :! :n
-- :doc clear history actions! use it carefully!
DELETE FROM stage_action WHERE stage = :stage

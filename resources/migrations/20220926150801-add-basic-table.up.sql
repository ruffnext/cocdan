CREATE TABLE users
(id INTEGER PRIMARY KEY AUTOINCREMENT,
 username VARCHAR(120) UNIQUE,
 nickname VARCHAR(120));
--;;
CREATE TABLE transactions
(id INTEGER,
 ctx_id INTEGER,
 user INTEGER,
 stage INTEGER,
 time VARCHAR(32),
 type VARCHAR(60),
 payload BLOB);
--;;
CREATE TABLE contexts
(id INTEGER,
 stage INTEGER,
 time VARCHAR(32),
 payload BLOB);
--;;
CREATE TABLE avatars
(id INTEGER PRIMARY KEY AUTOINCREMENT,
 name VARCHAR(120),
 image VARCHAR(120),
 description TEXT,
 stage INTEGER,
 substage VARCHAR(120),
 controlled_by INTEGER,
 payload BLOB);
--;;
CREATE TABLE stages
(id INTEGER PRIMARY KEY AUTOINCREMENT,
 name VARCHAR(120),
 introduction TEXT,
 image VARCHAR(120),
 substages BLOB,  -- 一个列表，存 id
 avatars BLOB,  -- 一个列表，存 id
 controlled_by INTEGER -- 由 user 控制
 );
--;;
CREATE TABLE substages
(id VARCHAR(120),
 name VARCHAR(250),
 adjacencies BLOB, -- 一个列表，存 substage id
 payload BLOB);
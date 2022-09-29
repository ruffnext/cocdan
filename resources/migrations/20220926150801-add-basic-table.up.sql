CREATE TABLE users
(id INTEGER PRIMARY KEY AUTOINCREMENT,
 username VARCHAR(120) UNIQUE,
 nickname VARCHAR(120));
--;;
CREATE TABLE transactions
(id INTEGER,
 stage INTEGER,
 type VARCHAR(30),
 props TEXT);
--;;
CREATE TABLE avatars
(id INTEGER PRIMARY KEY AUTOINCREMENT,
 name VARCHAR(120),
 image VARCHAR(120),
 description TEXT,
 stage INTEGER,
 substage VARCHAR(120),
 controlled_by INTEGER,
 props BLOB);
--;;
CREATE TABLE stages
(id INTEGER PRIMARY KEY AUTOINCREMENT,
 name VARCHAR(120),
 introduction TEXT,
 image VARCHAR(120),
 substages BLOB,  -- 一个列表，存 id
 avatars BLOB,  -- 一个列表，存 id
 controlled_by INTEGER);
--;;
CREATE TABLE substages
(id VARCHAR(120),
 name VARCHAR(250),
 adjacencies BLOB, -- 一个列表，存 substage id
 props BLOB);
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    header VARCHAR(100) NOT NULL DEFAULT '/img/header.png',
    email VARCHAR(100) NOT NULL UNIQUE,
    config JSON NOT NULL DEFAULT '{}'
);
--;;
CREATE TABLE IF NOT EXISTS avatars(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    header VARCHAR(100) NOT NULL DEFAULT '/img/avatar.png',
    controlled_by INTEGER NOT NULL,
    on_stage INTEGER,
    attributes JSON NOT NULL DEFAULT '{}' FORMAT JSON,
    FOREIGN KEY (controlled_by) REFERENCES users(id)
);
--;;
CREATE TABLE IF NOT EXISTS stages(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    owned_by INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'prelude',
    introduction VARCHAR(1024) NOT NULL DEFAULT '',
    banner VARCHAR(200) NOT NULL DEFAULT '/img/stage.jpg',
    code VARCHAR(32) NOT NULL DEFAULT '',
    attributes JSON NOT NULL DEFAULT '{"modules": []}' FORMAT JSON,
    FOREIGN KEY (owned_by) REFERENCES avatars(id)
);
--;;
CREATE TABLE IF NOT EXISTS messages
(id INTEGER PRIMARY KEY AUTO_INCREMENT,
 content TEXT NOT NULL,
 submitted_by INTEGER NOT NULL,
 submitted_on DATE NOT NULL,
 FOREIGN KEY (submitted_by) REFERENCES avatars(id));
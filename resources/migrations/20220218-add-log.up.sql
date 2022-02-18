CREATE TABLE IF NOT EXISTS avatar_messages (
    id INTEGER PRIMARY KEY,
    messages JSON NOT NULL DEFAULT '[]' FORMAT JSON,
    FOREIGN KEY (id) REFERENCES avatars(id)
);
CREATE TABLE IF NOT EXISTS stage_action (
    `order` INTEGER NOT NULL,
    `type` VARCHAR(64) NOT NULL,
    `fact` JSON NOT NULL,
    `time` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `stage` INTEGER NOT NULL,
    FOREIGN KEY (`stage`) REFERENCES stages(id)
);
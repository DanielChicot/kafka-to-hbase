CREATE TABLE IF NOT EXISTS `ucfs` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `hbase_id` VARCHAR(2048) NULL,
    `hbase_timestamp` BIGINT NULL,
    `write_timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `correlation_id` VARCHAR(160) NULL,
    `topic_name` VARCHAR(160) NULL,
    `kafka_partition` INT NULL,
    `kafka_offset` INT NULL,
    `reconciled_result` TINYINT(1) NOT NULL DEFAULT 0,
    `reconciled_timestamp` DATETIME NULL,
    PRIMARY KEY (`id`),
    INDEX (hbase_id,hbase_timestamp),
    INDEX (write_timestamp),
    INDEX (reconciled_result)
);

CREATE TABLE `miasha`.`order_info` (
  `id` VARCHAR(32) NOT NULL,
  `user_id` INT(11) NOT NULL DEFAULT 0,
  `item_id` INT(11) NOT NULL DEFAULT 0,
  `item_price` DOUBLE NOT NULL DEFAULT 0,
  `amount` INT(11) NOT NULL DEFAULT 0,
  `order_price` DOUBLE NULL DEFAULT 0,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci
COMMENT = '下单交易表';
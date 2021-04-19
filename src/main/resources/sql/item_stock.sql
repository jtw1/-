CREATE TABLE `miasha`.`item_stock` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `stock` INT NOT NULL DEFAULT 0,
  `item_id` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci
COMMENT = '库存表';
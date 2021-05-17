CREATE TABLE `miasha`.`stock_log` (
  `stock_log_id` VARCHAR(64) NOT NULL,
  `item_id` INT NOT NULL DEFAULT 0,
  `amount` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`stock_log_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci
COMMENT = '库存流水';

ALTER TABLE `miasha`.`stock_log`
ADD COLUMN `status` INT NOT NULL DEFAULT 0 COMMENT '1表示初始状态\n2表示下单扣减库存成功\n3表示下单回滚' AFTER `amount`;
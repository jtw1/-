CREATE TABLE `miasha`.`sequence_info` (
  `name` VARCHAR(255) NOT NULL,
  `current_value` INT NOT NULL DEFAULT 0,
  `step` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`name`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci
COMMENT = '初始化进去一些sequence，初始值是0，每当从sequence获取数据时，就加上对应的步长';

INSERT INTO `miasha`.`sequence_info` (`name`, `current_value`, `step`) VALUES ('order_info', '0', '1');
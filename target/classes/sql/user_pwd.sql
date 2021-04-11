CREATE TABLE `miasha`.`user_pwd` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `encrpt_password` VARCHAR(128) NOT NULL DEFAULT '',
  `user_id` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`));
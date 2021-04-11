CREATE TABLE `miasha`.`user_info` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  `gender` INT(1) NOT NULL,
  `age` INT NOT NULL,
  `telephone` VARCHAR(45) NOT NULL,
  `register_mode` VARCHAR(45) NOT NULL,
  `third_party_id` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`id`))
COMMENT = '用户表  \n gender：1 男性   2女性\nregister_mode: by telephone//wechat';
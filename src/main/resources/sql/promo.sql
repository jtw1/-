CREATE TABLE `promo` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `promo_name` varchar(255) COLLATE utf8_unicode_ci NOT NULL DEFAULT '' COMMENT '//秒杀活动商品名称',
  `start_time` datetime NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '//活动开始时间',
  `item_id` int(11) NOT NULL DEFAULT '0' COMMENT '//秒杀活动商品id',
  `promo_item_price` double NOT NULL DEFAULT '0' COMMENT '//秒杀活动商品价格',
  `end_time` datetime NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '//秒杀活动结束时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
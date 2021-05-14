package org.example.service;

import org.example.error.BusinessException;
import org.example.service.model.OrderModel;

/**
 * @Description 处理订单交易
 * @date 2021/4/21-21:09
 */
public interface OrderService {
    //推荐使用：1.通过前端url上传过来秒杀活动id，然后下单接口内校验对应id是否属于对应商品且活动已开始
    //2.直接在下单接口内判断对应商品是否存在秒杀活动，若存在则以秒杀价格下单

    /**
     *
     * @param userId 下单用户
     * @param itemId  用户购买的商品id
     * @param promoId  秒杀活动商品id
     * @param amount  用户购买的商品数量
     * @return
     */
    OrderModel createOrder(Integer userId,Integer itemId,Integer promoId,Integer amount) throws BusinessException;


}

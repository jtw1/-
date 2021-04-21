package org.example.service;

import org.example.error.BusinessException;
import org.example.service.model.OrderModel;

/**
 * @Description 处理订单交易
 * @date 2021/4/21-21:09
 */
public interface OrderService {
    /**
     *
     * @param userId 下单用户
     * @param itemId  用户购买的商品id
     * @param amount  用户购买的商品数量
     * @return
     */
    OrderModel createOrder(Integer userId,Integer itemId,Integer amount) throws BusinessException;


}

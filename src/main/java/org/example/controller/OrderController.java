package org.example.controller;

import org.example.error.BusinessException;
import org.example.response.CommonReturnType;
import org.example.service.OrderService;
import org.example.service.model.OrderModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * @Description 用于下单的一个外部controller层接入
 * @date 2021/4/21-23:09
 */
@Controller("/order")
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true",origins = {"*"})     //解决跨域问题
public class OrderController extends BaseController{
    @Autowired
    private OrderService orderService;

    //封装下单请求
    @RequestMapping(value = "/createorder",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name="itemId")Integer itemId,
                                        @RequestParam(name="amount")Integer amount) throws BusinessException {
        OrderModel orderModel = orderService.createOrder(null, itemId, amount);
        return CommonReturnType.create(null);
    }
}

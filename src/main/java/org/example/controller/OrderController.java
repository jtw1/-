package org.example.controller;

import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.response.CommonReturnType;
import org.example.service.OrderService;
import org.example.service.model.OrderModel;
import org.example.service.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

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
    @Autowired
    private HttpServletRequest httpServletRequest;

    //封装下单请求  promoId如果·不传递的话(required = false)认定是以平销价格
    @RequestMapping(value = "/createorder",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name="itemId")Integer itemId,
                                        @RequestParam(name="amount")Integer amount,
                                        @RequestParam(name="promoId",required = false)Integer promoId) throws BusinessException {
        //判断用户是否登录
        Boolean isLogin= (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if (isLogin==null || !isLogin.booleanValue()){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录，不能下单");
        }
        //获取用户登录信息
        UserModel userModel= (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USR");

        OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId,amount);
        return CommonReturnType.create(null);
    }
}

package org.example.controller;

import com.google.common.util.concurrent.RateLimiter;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.mq.MqProducer;
import org.example.response.CommonReturnType;
import org.example.service.ItemService;
import org.example.service.OrderService;
import org.example.service.PromoService;
import org.example.service.model.OrderModel;
import org.example.service.model.UserModel;
import org.example.util.CodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

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
    @Autowired
    private MqProducer mqProducer;
    @Autowired
    private ItemService itemService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init(){
        executorService = Executors.newFixedThreadPool(20);
        orderCreateRateLimiter = RateLimiter.create(300);
    }

    //生成验证码
    @RequestMapping(value = "/generateverifycode",method = {RequestMethod.POST})
    @ResponseBody
    public void generateVerifyCode(HttpServletResponse httpServletResponse) throws BusinessException, IOException {
        //根据token获取用户信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能生成验证码");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能生成验证码");
        }

        Map<String, Object> map = CodeUtil.generateCodeAndPic();
        redisTemplate.opsForValue().set("verify_code_"+userModel.getId(),map.get("code"));
        redisTemplate.expire("verify_code_"+userModel.getId(),10,TimeUnit.MINUTES);

        ImageIO.write((RenderedImage) map.get("codePic"), "jpeg", httpServletResponse.getOutputStream());

    }


    //生成秒杀令牌
    @RequestMapping(value = "/generatetoken",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType generateToken(@RequestParam(name="itemId")Integer itemId,
                                        @RequestParam(name="promoId")Integer promoId,
                                        @RequestParam(name="verifyCode")String verifyCode) throws BusinessException {
        //根据token获取用户信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        }
        //获取用户的登陆信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        }

        //通过verifyCode验证 验证码的有效性
        String redisVerifyCode = (String) redisTemplate.opsForValue().get("verify_code_" + userModel.getId());

        //获取秒杀访问令牌
        String promoToken = promoService.generateSecondKillToken(promoId,itemId,userModel.getId());
        if (StringUtils.isEmpty(redisVerifyCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"请求非法");
        }
        if (!redisVerifyCode.equalsIgnoreCase(verifyCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"请求非法,验证码错误");
        }

        if(promoToken == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"生成令牌失败");
        }
        //返回对应的结果
        return CommonReturnType.create(promoToken);
    }


    //封装下单请求  promoId如果·不传递的话(required = false)认定是以平销价格
    @RequestMapping(value = "/createorder",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name="itemId")Integer itemId,
                                        @RequestParam(name="amount")Integer amount,
                                        @RequestParam(name="promoId",required = false)Integer promoId,
                                        @RequestParam(name="promoToken",required = false)String promoToken) throws BusinessException {
        //令牌桶验证
        if (!orderCreateRateLimiter.tryAcquire()){
            throw new BusinessException(EmBusinessError.RATE_LIMIT);
        }

        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        }
        //获取用户的登陆信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        }

        //校验秒杀令牌是否正确
        if (promoId!=null){
            String inRedisPromoToken = (String) redisTemplate.opsForValue().get("promo_token_"+promoId+"_user_id_"+userModel.getId()+"_item_id_"+itemId);
            if (inRedisPromoToken==null){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌校验失败");
            }

            if (!org.apache.commons.lang3.StringUtils.equals(promoToken,inRedisPromoToken)){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌校验失败");
            }
        }

        //校验秒杀令牌是否正确
        if(promoId != null){
            String inRedisPromoToken = (String) redisTemplate.opsForValue().get("promo_token_"+promoId+"_userid_"+userModel.getId()+"_itemid_"+itemId);
            if(inRedisPromoToken == null){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌校验失败");
            }
            if(!org.apache.commons.lang3.StringUtils.equals(promoToken,inRedisPromoToken)){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌校验失败");
            }
        }

        //同步调用线程池的submit方法
        //拥塞窗口为20的等待队列，用来队列化泄洪
        Future<Object> future = executorService.submit(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                //加入库存流水init状态
                String stockLogId = itemService.initStockLog(itemId,amount);


                //再去完成对应的下单事务型消息机制
                if(!mqProducer.transactionAsyncReduceStock(userModel.getId(),itemId,promoId,amount,stockLogId)){
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR,"下单失败");
                }
                return null;
            }
        });

        try {
            future.get();
        } catch (InterruptedException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        } catch (ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }

        return CommonReturnType.create(null);


//        Boolean isLogin= (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
//        if (isLogin==null || !isLogin.booleanValue()){
//            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录，不能下单");
//        }
//        //获取用户登录信息
//        UserModel userModel= (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USR");
//
//        //OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId,amount);
//
//        //判断库存是否已售罄，若对应的售罄存在，直接返回下单失败
//        if (redisTemplate.hasKey("promo_item_stock_invalid_"+itemId)){
//            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
//        }
//        //加入库存流水init状态
//        String stockLogId =itemService.initStockLog(itemId,amount);
//
//        //完成对应的下单事务型消息机制
//        if (!mqProducer.transactionAsyncReduceStock(userModel.getId(),itemId,promoId,amount,stockLogId)){
//            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
//        }
//        return CommonReturnType.create(null);
    }
}

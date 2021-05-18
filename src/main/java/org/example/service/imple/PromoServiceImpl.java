package org.example.service.imple;

import org.example.DataObject.PromoDo;
import org.example.dao.PromoDoMapper;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.service.ItemService;
import org.example.service.PromoService;
import org.example.service.UserService;
import org.example.service.model.ItemModel;
import org.example.service.model.PromModel;
import org.example.service.model.PromoModel;
import org.example.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @date 2021/4/24-16:13
 */
@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDoMapper promoDoMapper;
    @Autowired
    private ItemService itemService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        PromoDo promoDo = promoDoMapper.selectByItemId(itemId);

        //dataObject->model
        PromoModel promoModel=convertFromDateObject(promoDo);
        if (promoModel==null) return null;

        //判断当前时间是否有秒杀活动即将开始或者正在进行

        //秒杀活动开始时间晚于当前时间，表示未开始
        if (promoModel.getStartTime().isAfterNow()){
            promoModel.setStatus(1);
        }//秒杀活动结束时间早于当前时间，表示以结束
        else if(promoModel.getEndTime().isBeforeNow()){
            promoModel.setStatus(3);
        }else{
            promoModel.setStatus(2);
        }

        return promoModel;
    }

    /**
     * 活动发布时，同步库存到redis缓存（此时忽略活动发布时有下单行为的情况）
     * @param promoId
     */
    @Override
    public void publishPromo(Integer promoId) {
        //通过活动id获取活动
        PromoDo promoDo=promoDoMapper.selectByPrimaryKey(promoId);
        if (promoDo.getItemId()==null || promoDo.getItemId().intValue()==0) return;

        ItemModel itemModel = itemService.getItemById(promoDo.getItemId());

        //将库存同步到redis
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(),itemModel.getStock());

        //将大闸限制数字设到redis内
        redisTemplate.opsForValue().set("promo_door_count"+promoId,itemModel.getStock().intValue()*5);

    }

    /**
     * 生成秒杀令牌
     * @param promoId
     * @return
     */
    @Override
    public String generateSecondKillToken(Integer promoId,Integer itemId,Integer userId) throws BusinessException {
        //判断库存是否已售罄   若对应的售罄key存在，则直接返回下单失败
        if (redisTemplate.hasKey("promo_item_stock_invalid_"+itemId)) return null;

        //获取对应商品的秒杀活动信息
        PromoDo promoDo = promoDoMapper.selectByPrimaryKey(promoId);

        //dataObject->model
        PromoModel promoModel=convertFromDateObject(promoDo);
        if (promoModel==null) return null;

        //判断当前时间是否有秒杀活动即将开始或者正在进行

        //秒杀活动开始时间晚于当前时间，表示未开始
        if (promoModel.getStartTime().isAfterNow()){
            promoModel.setStatus(1);
        }//秒杀活动结束时间早于当前时间，表示以结束
        else if(promoModel.getEndTime().isBeforeNow()){
            promoModel.setStatus(3);
        }else{
            promoModel.setStatus(2);
        }
        //判断活动是否正在进行
        if (promoModel.getStatus().intValue()!=2){
            return null;
        }

        //校验下单状态，下单商品是否存在，用户是否合法
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel==null){
            return null;
        }
        UserModel userModel = userService.getUserByIdInCache(userId);
        if (userModel==null){
            return null;
        }

        //获取秒杀大闸的count数量
        long result=redisTemplate.opsForValue().increment("promo_door_count"+promoId,-1);
        if (result<0){
            return null;
        }

        //生成token并存入redis内，并给一个5分钟的有效期
        String token= UUID.randomUUID().toString().replace("-","");
        redisTemplate.opsForValue().set("promo_token_"+promoId+"_user_id_"+userId+"_item_id_"+itemId,token);
        redisTemplate.expire("promo_token_"+promoId+"_user_id_"+userId+"_item_id_"+itemId,5, TimeUnit.MINUTES);
        return token;
    }


    private PromoModel convertFromDateObject(PromoDo promoDo){
        if(promoDo==null){
            return null;
        }
        PromoModel promoModel=new PromoModel();
        BeanUtils.copyProperties(promoDo,promoModel);

        promoModel.setPromoItemPrice(new BigDecimal(promoDo.getPromoItemPrice()));
        promoModel.setStartTime(new DateTime(promoDo.getStartTime()));
        promoModel.setEndTime(new DateTime(promoDo.getEndTime()));

        return promoModel;
    }
}

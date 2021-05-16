package org.example.service.imple;

import org.example.DataObject.PromoDo;
import org.example.dao.PromoDoMapper;
import org.example.service.ItemService;
import org.example.service.PromoService;
import org.example.service.model.ItemModel;
import org.example.service.model.PromModel;
import org.example.service.model.PromoModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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

package org.example.service.imple;

import org.example.DataObject.PromoDo;
import org.example.dao.PromoDoMapper;
import org.example.service.PromoService;
import org.example.service.model.PromModel;
import org.example.service.model.PromoModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

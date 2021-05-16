package org.example.service;

import org.example.service.model.PromModel;
import org.example.service.model.PromoModel;

/**
 * @Description
 * @date 2021/4/24-16:11
 */
public interface PromoService {
    //根据itemId获取即将进行的或正在进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId);

    //活动发布
    void publishPromo(Integer promoId);
}

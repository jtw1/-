package org.example.service;

import org.example.service.model.PromModel;

/**
 * @Description
 * @date 2021/4/24-16:11
 */
public interface PromoService {
    PromModel getPromoByItemId(Integer itemId);
}

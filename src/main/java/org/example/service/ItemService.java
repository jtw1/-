package org.example.service;

import org.example.error.BusinessException;
import org.example.service.model.ItemModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description
 * @date 2021/4/19-22:06
 */
@Service
public interface ItemService {
    //创建商品
    ItemModel creatItem(ItemModel itemModel) throws BusinessException;

    //商品列表浏览
    List<ItemModel> listItem();

    //商品详情浏览
    ItemModel getItemById(Integer id);
}

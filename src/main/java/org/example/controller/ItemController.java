package org.example.controller;

import org.example.controller.viewObject.ItemVO;
import org.example.error.BusinessException;
import org.example.response.CommonReturnType;
import org.example.service.ItemService;
import org.example.service.model.ItemModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * @Description
 * @date 2021/4/19-23:13
 */
@Controller("/item")
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")     //解决跨域问题
public class ItemController extends BaseController{
    @Autowired
    private ItemService itemService;

    /**
     * 创建item
     * @param title
     * @param description
     * @param price
     * @param stock
     * @param imgUrl
     * @return
     */
    public CommonReturnType createItem(@RequestParam(name="title")String title,
                                       @RequestParam(name="description")String description,
                                       @RequestParam(name="price") BigDecimal price,
                                       @RequestParam(name="stock")Integer stock,
                                       @RequestParam(name="imgUrl")String imgUrl) throws BusinessException {
        //封装service请求用来创建商品
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setStock(stock);
        itemModel.setDescription(description);
        itemModel.setPrice(price);
        itemModel.setImgUrl(imgUrl);

        ItemModel itemModelForReturn = itemService.creatItem(itemModel);
        ItemVO itemVO=convertVOFromModel(itemModelForReturn);

        return CommonReturnType.create(itemVO);
    }

    /**
     * itemModel->ItemVO
     * @param itemModel
     * @return
     */
    private ItemVO convertVOFromModel(ItemModel itemModel){
        if (itemModel==null){
            return null;
        }

        ItemVO itemVO=new ItemVO();
        BeanUtils.copyProperties(itemModel,itemVO);
        return itemVO;
    }
}

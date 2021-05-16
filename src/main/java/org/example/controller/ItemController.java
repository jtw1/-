package org.example.controller;

import org.example.controller.viewObject.ItemVO;
import org.example.error.BusinessException;
import org.example.response.CommonReturnType;
import org.example.service.CacheService;
import org.example.service.ItemService;
import org.example.service.PromoService;
import org.example.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private PromoService promoService;

    /**
     * 创建item
     * @param title
     * @param description
     * @param price
     * @param stock
     * @param imgUrl
     * @return
     */
    @RequestMapping(value = "/create",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
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
        itemModel.setImgurl(imgUrl);

        ItemModel itemModelForReturn = itemService.creatItem(itemModel);
        ItemVO itemVO=convertVOFromModel(itemModelForReturn);

        return CommonReturnType.create(itemVO);
    }

    /**
     * 商品详情页浏览
     * @param id
     * @return
     */
    @RequestMapping(value = "/get",method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name="id")Integer id){
        ItemModel itemModel= null;
        //先取本地缓存
        itemModel=(ItemModel)cacheService.getFromCommonCache("item_"+id);

        //若本地缓存不存在
        if (itemModel==null){
            //再取redis中的，根据商品id到redis内获取
            itemModel = (ItemModel)redisTemplate.opsForValue().get("item_"+id);

            //若redis内不存在对应的itemModel，则访问下游service
            if (itemModel==null){
                itemModel = itemService.getItemById(id);
                //设置itemModel到redis内，并加一个过期时间
                redisTemplate.opsForValue().set("item_"+id,itemModel);
                redisTemplate.expire("item_"+id,10, TimeUnit.MINUTES);
            }
            //填充本地缓存
            cacheService.setCommonCache("item_"+id,itemModel);
        }

        ItemVO itemVO=convertVOFromModel(itemModel);

        return CommonReturnType.create(itemVO);
    }

    /**
     * 商品列表页浏览
     * @return
     */
    @RequestMapping(value = "/list",method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType listItem(){
        List<ItemModel> itemModelList=itemService.listItem();
        //使用Stream API将list内的itemModel转化为 itemVO
        List<ItemVO> itemVOList=itemModelList.stream().map(itemModel -> {
            ItemVO itemVO=this.convertVOFromModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);
    }

    @RequestMapping(value = "/publishpromo",method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType publishPromo(@RequestParam(name="id")Integer id){
        promoService.publishPromo(id);
        return CommonReturnType.create(null);
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

        if (itemModel.getPromoModel()!=null){
            //有正在进行或即将进行的秒杀活动
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setStartTime(itemModel.getPromoModel().getStartTime().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else{
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }
}

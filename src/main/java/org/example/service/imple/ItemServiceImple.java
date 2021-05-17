package org.example.service.imple;

import org.example.DataObject.ItemDo;
import org.example.DataObject.ItemStockDo;
import org.example.DataObject.StockLogDo;
import org.example.dao.ItemDoMapper;
import org.example.dao.ItemStockDoMapper;
import org.example.dao.StockLogDoMapper;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.mq.MqProducer;
import org.example.service.ItemService;
import org.example.service.PromoService;
import org.example.service.model.ItemModel;
import org.example.service.model.PromoModel;
import org.example.validator.ValidationResult;
import org.example.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description
 * @date 2021/4/19-22:11
 */
@Service
public class ItemServiceImple implements ItemService {
    @Autowired
    private ValidatorImpl validator;
    @Autowired
    private ItemDoMapper itemDoMapper;
    @Autowired
    private ItemStockDoMapper itemStockDoMapper;
    @Autowired
    private PromoService promoService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MqProducer mqProducer;
    @Autowired
    private StockLogDoMapper stockLogDoMapper;

    @Override
    @Transactional  //创建item必须在同一个事务当中
    public ItemModel creatItem(ItemModel itemModel) throws BusinessException {
        //校验入参
        ValidationResult result = validator.validate(itemModel);
        if (result.isHasErrors()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,result.getErrorMsg());
        }
        //转化itemModel->dataObject
        ItemDo itemDo=this.convertToItemDoFromItemModel(itemModel);

        //写入数据库
        itemDoMapper.insertSelective(itemDo);
        itemModel.setId(itemDo.getId());

        ItemStockDo itemStockDo=this.convertToItemStockDoFromItemModel(itemModel);
        itemStockDoMapper.insertSelective(itemStockDo);
        //返回创建完成的对象
        return this.getItemById(itemModel.getId());
    }

    /**
     * 商品列表浏览
     * @return
     */
    @Override
    public List<ItemModel> listItem() {
        List<ItemDo> itemDoList=itemDoMapper.listItem();
        List<ItemModel> itemModelList=itemDoList.stream().map(itemDo -> {
            ItemStockDo itemStockDo=itemStockDoMapper.selectByItemId(itemDo.getId());
            ItemModel itemModel=this.convertToModelFromDataObject(itemDo,itemStockDo);
            return itemModel;
        }).collect(Collectors.toList());
        return itemModelList;
    }

    @Override
    public ItemModel getItemById(Integer id) {
        ItemDo itemDo = itemDoMapper.selectByPrimaryKey(id);
        if (itemDo==null){
            return null;
        }
        //操作获得库存数量
        ItemStockDo itemStockDo = itemStockDoMapper.selectByItemId(itemDo.getId());

        //将dataObject->model
        ItemModel itemModel=convertToModelFromDataObject(itemDo,itemStockDo);

        //获取活动商品信息
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());
        if (promoModel!=null && promoModel.getStatus().intValue()!=3){
            itemModel.setPromoModel(promoModel);
        }
        return itemModel;
    }

    /**
     * 从缓存中获取 itemModel
     * @param id
     * @return
     */
    @Override
    public ItemModel getItemByIdInCache(Integer id) {
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_validate_"+ id);
        if (itemModel==null){
            itemModel=this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_" + id,itemModel);
            //设置有效期
            redisTemplate.expire("item_validate_" + id,10, TimeUnit.MINUTES);
        }
        return itemModel;
    }

    /**
     * 库存扣减
     * @param itemId 商品id
     * @param amount 需要的商品数量
     * @return
     */
    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        //int affectedRow = itemStockDoMapper.decreaseStock(itemId, amount);
        long result=redisTemplate.opsForValue().increment("promo_item_stock_"+itemId,amount.intValue()*(-1));
        //true：更新库存成功    false：更新库存失败
        if(result >0){
            //更新库存成功
            return true;
        }else if(result == 0){
            //打上库存已售罄的标识
            redisTemplate.opsForValue().set("promo_item_stock_invalid_"+itemId,"true");
            return true;
        }else{
            //更新库存失败
            increaseStock(itemId,amount);
            return false;
        }
    }

    /**
     * 下单成功之后，商品销量增加amount
     * @param itemId 商品id
     * @param amount 商品下单数量
     * @throws BusinessException
     */
    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {
        itemDoMapper.increaseSales(itemId,amount);
    }

    //库存回补
    @Override
    public boolean increaseStock(Integer itemId, Integer amount) throws BusinessException {
        redisTemplate.opsForValue().increment("promo_item_stock_"+itemId,amount.intValue());
        return true;
    }

    //异步更新库存
    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {
        boolean mqResult = mqProducer.asyncReduceStock(itemId,amount);
        return mqResult;
    }

    /**
     * 初始化对应的库存流水
     * @param itemId
     * @param amount
     * @return
     */
    @Override
    public String initStockLog(Integer itemId, Integer amount) {
        StockLogDo stockLogDo = new StockLogDo();
        stockLogDo.setItemId(itemId);
        stockLogDo.setAmount(amount);

        stockLogDo.setStockLogId(UUID.randomUUID().toString().replace("-", ""));
        stockLogDo.setStatus(1);

        stockLogDoMapper.insertSelective(stockLogDo);
        return stockLogDo.getStockLogId();
    }

    /**
     * 实现 ItemModel->ItemDo
     * @param itemModel
     * @return
     */
    private ItemDo convertToItemDoFromItemModel(ItemModel itemModel){
        if (itemModel==null){
            return null;
        }
        ItemDo itemDo = new ItemDo();
        BeanUtils.copyProperties(itemModel,itemDo);
        //类型不一样的Property不会复制  price，所以需要手动复制
        itemDo.setPrice(itemModel.getPrice().doubleValue());
        return itemDo;
    }

    /**
     * 实现 ItemModel->ItemStockDo
     * @param itemModel
     * @return
     */
    private ItemStockDo convertToItemStockDoFromItemModel(ItemModel itemModel){
        if (itemModel==null){
            return null;
        }
        ItemStockDo itemStockDo = new ItemStockDo();
        itemStockDo.setItemId(itemModel.getId());
        itemStockDo.setStock(itemModel.getStock());
        return itemStockDo;
    }

    /**
     * 将dataObject->model
     * @param itemDo
     * @param itemStockDo
     * @return
     */
    private ItemModel convertToModelFromDataObject(ItemDo itemDo,ItemStockDo itemStockDo){
        ItemModel itemModel = new ItemModel();

        BeanUtils.copyProperties(itemDo,itemModel);
        itemModel.setPrice(new BigDecimal(itemDo.getPrice()));
        itemModel.setStock(itemStockDo.getStock());

        return itemModel;
    }
}

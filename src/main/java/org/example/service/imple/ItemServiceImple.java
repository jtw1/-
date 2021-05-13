package org.example.service.imple;

import org.example.DataObject.ItemDo;
import org.example.DataObject.ItemDoExample;
import org.example.DataObject.ItemStockDo;
import org.example.dao.ItemDoMapper;
import org.example.dao.ItemStockDoMapper;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.service.ItemService;
import org.example.service.PromoService;
import org.example.service.model.ItemModel;
import org.example.service.model.PromoModel;
import org.example.validator.ValidationResult;
import org.example.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
     * 库存扣减
     * @param itemId 商品id
     * @param amount 需要的商品数量
     * @return
     */
    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) {
        int affectedRow = 0;
        try {
            affectedRow = itemStockDoMapper.decreaseStock(itemId, amount);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //true：更新库存成功    false：更新库存失败
        return affectedRow>0?true:false;
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

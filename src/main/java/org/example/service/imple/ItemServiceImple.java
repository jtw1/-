package org.example.service.imple;

import org.example.DataObject.ItemDo;
import org.example.DataObject.ItemDoExample;
import org.example.DataObject.ItemStockDo;
import org.example.dao.ItemDoMapper;
import org.example.dao.ItemStockDoMapper;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.service.ItemService;
import org.example.service.model.ItemModel;
import org.example.validator.ValidationResult;
import org.example.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description
 * @date 2021/4/19-22:11
 */
public class ItemServiceImple implements ItemService {
    @Autowired
    private ValidatorImpl validator;
    @Autowired
    private ItemDoMapper itemDoMapper;
    @Autowired
    private ItemStockDoMapper itemStockDoMapper;

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

    @Override
    public List<ItemModel> listItem() {
        return null;
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
        return itemModel;
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

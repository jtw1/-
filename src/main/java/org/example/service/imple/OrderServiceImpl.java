package org.example.service.imple;

import org.example.DataObject.OrderDo;
import org.example.DataObject.SequenceDo;
import org.example.DataObject.StockLogDo;
import org.example.dao.OrderDoMapper;
import org.example.dao.SequenceDoMapper;
import org.example.dao.StockLogDoMapper;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.service.ItemService;
import org.example.service.OrderService;
import org.example.service.UserService;
import org.example.service.model.ItemModel;
import org.example.service.model.OrderModel;
import org.example.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @Description
 * @date 2021/4/21-21:11
 */
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderDoMapper orderDoMapper;
    @Autowired
    private SequenceDoMapper sequenceDoMapper;
    @Autowired
    private StockLogDoMapper stockLogDoMapper;

    /**
     * 创建订单
     * @param userId 下单用户
     * @param itemId  用户购买的商品id
     * @param promoId  秒杀活动商品id
     * @param amount  用户购买的商品数量
     * @return
     */
    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId,Integer promoId, Integer amount,String stockLogId) throws BusinessException {
       //校验下单状态，下单商品是否存在，用户是否合法，购买数量是否正确
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");
        }
        UserModel userModel = userService.getUserByIdInCache(userId);
        if (userModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户信息不存在");
        }
        if (amount<=0 || amount>99){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"数量信息不正确");
        }

        //校验活动信息
        if (promoId!=null){
            //1.校验对应活动是否存在这个适用商品
            if (promoId.intValue()!=itemModel.getPromoModel().getId()){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
            }//2.校验活动是否正在进行中
            else if (itemModel.getPromoModel().getStatus()!=2){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动还未开始");
            }

        }

        //落单减库存，支付减库存
        boolean result=itemService.decreaseStock(itemId,amount);
        if(!result){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }
        //订单入库
        OrderModel orderModel=new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        if (promoId!=null){
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else{
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setPromoId(promoId);
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));

        // 生成订单号
        orderModel.setId(generateOrderNumber());
        OrderDo orderDo=convertFromOrderModel(orderModel);
        orderDoMapper.insertSelective(orderDo);


        //加上商品销量
        itemService.increaseSales(itemId,amount);

        //设置库存流水状态为成功
        StockLogDo stockLogDo=stockLogDoMapper.selectByPrimaryKey(stockLogId);
        if (stockLogDo==null){
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }
        stockLogDo.setStatus(2);  //2表示下单库存扣减成功
        //更新数据库
        stockLogDoMapper.updateByPrimaryKeySelective(stockLogDo);

//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//            //最近的一个带@Transactional的事务成功commit之后才回去执行
//            @Override
//            public void afterCommit() {
//                //异步更新库存
//                boolean mqResult=itemService.asyncDecreaseStock(itemId,amount);
////                if (!mqResult){
////                    itemService.increaseStock(itemId,amount);
////                    throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
////                }
//            }
//        });

        //返回前端
        return orderModel;
    }

    /**
     * 生成订单号
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)   //保证同一个sequence不会重复使用
    String generateOrderNumber(){
        //订单号有16位
        StringBuilder res=new StringBuilder();
        //前八位是时间信息 年月日
        LocalDateTime now=LocalDateTime.now();
        String nowDate=now.format(DateTimeFormatter.ISO_DATE).replace("-","");
        res.append(nowDate);

        //中间6位是自增序列
        //获取当前sequence
        int sequence=0;
        SequenceDo sequenceDo = sequenceDoMapper.getSequenceByName("order_info");
        sequence=sequenceDo.getCurrentValue();
        sequenceDo.setCurrentValue(sequenceDo.getCurrentValue()+sequenceDo.getStep());
        sequenceDoMapper.updateByPrimaryKeySelective(sequenceDo);

        String sequenceStr=String.valueOf(sequence);
        for (int i = 0; i < 6 - sequenceStr.length(); i++) {
            res.append(0);
        }
        res.append(sequenceStr);

        //最后两位是分库分表位,暂时写死，开发中也可对表的数目取余数
        res.append("00");

        return res.toString();
    }

    /**
     * orderModel->dataObject
     * @param orderModel
     * @return
     */
    private OrderDo convertFromOrderModel(OrderModel orderModel){
        if (orderModel==null){
            return null;
        }
        OrderDo orderDo = new OrderDo();
        BeanUtils.copyProperties(orderModel,orderDo);
        orderDo.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDo.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDo;
    }
}

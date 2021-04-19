package org.example.controller.viewObject;

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @Description 用于给前端用户展示的item
 * @date 2021/4/19-23:15
 */
public class ItemVO {
    private Integer id;
    //商品名称

    private String title;
    //价格

    private BigDecimal price;
    //这里price是BigDecimal类型，而itemDo里面的price是double类型，是因为double类型的数据传到前端，被jsonStringBy之后可能存在精度问题

    //库存

    private Integer stock;
    //商品描述

    private String description;
    //销量
    private Integer sales;
    //商品描述的url

    private String imgUrl;
}

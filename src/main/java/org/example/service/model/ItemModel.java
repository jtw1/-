package org.example.service.model;

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @Description
 * @date 2021/4/19-20:22
 */
public class ItemModel {
    private Integer id;
    //商品名称
    @NotBlank(message = "商品名称不能为空")
    private String title;
    //价格
    @NotNull(message = "商品价格不能为空")
    @Min(value = 0,message = "商品价格必须大于0岁")
    private BigDecimal price;
    //这里price是BigDecimal类型，而itemDo里面的price是double类型，是因为double类型的数据传到前端，被jsonStringBy之后可能存在精度问题

    //库存
    @NotNull(message = "库存不能不填")
    private Integer stock;
    //商品描述
    @NotBlank(message = "商品描述信息不能为空")
    private String description;
    //销量
    private Integer sales;
    //商品描述的url
    @NotBlank(message = "图片信息不能为空")
    private String imgurl;

    //使用聚合模型,如果promoModel不为空，表示其拥有还未结束的秒杀活动
    private PromoModel promoModel;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public String getImgurl() {
        return imgurl;
    }

    public void setImgurl(String imgurl) {
        this.imgurl = imgurl;
    }

    public PromoModel getPromoModel() {
        return promoModel;
    }

    public void setPromoModel(PromoModel promoModel) {
        this.promoModel = promoModel;
    }
}

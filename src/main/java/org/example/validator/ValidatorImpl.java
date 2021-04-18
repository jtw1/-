package org.example.validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

/**
 * @Description
 * @date 2021/4/18-21:21
 */
@Component
public class ValidatorImpl implements InitializingBean {
    private Validator validator;

    /**
     * 实现校验方法并返回校验结果
     * @param bean
     * @return
     */
    public ValidationResult validate(Object bean){
        ValidationResult result = new ValidationResult();
        //若对应的bean里面的一些参数的规则有违背了对应validation定义的annotation的话，constraintViolationSet里面就会有这个值
        Set<ConstraintViolation<Object>> constraintViolationSet = validator.validate(bean);

        if (constraintViolationSet.size()>0){
            result.setHasErrors(true);
            constraintViolationSet.forEach(ConstraintViolation->{
                String errMsg=ConstraintViolation.getMessage();
                //获取发生错误的字段是哪个
                String propertyName=ConstraintViolation.getPropertyPath().toString();
                result.getErrorMsgMap().put(propertyName,errMsg);
            });
        }
        return result;
    }

    //当springBean初始化完成之后，会回调ValidatorImpl的afterPropertiesSet方法
    @Override
    public void afterPropertiesSet() throws Exception {
        //将hibernate validator通过工厂的初始化方式使其实例化
        this.validator= Validation.buildDefaultValidatorFactory().getValidator();
    }
}

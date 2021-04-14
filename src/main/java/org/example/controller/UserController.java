package org.example.controller;

import org.apache.tomcat.util.security.MD5Encoder;
import org.example.controller.viewObject.UserVO;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.response.CommonReturnType;
import org.example.service.UserService;
import org.example.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Random;

/**
 * @Description
 * @date 2021/4/12-22:08
 */
@Controller("user")
@RequestMapping("/user")
@CrossOrigin
public class UserController extends BaseController{
    @Autowired
    private UserService userService;
    @Autowired
    private HttpServletRequest httpServletRequest;

    //用户注册接口
    @RequestMapping(value = "/register",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name = "telephone")String telephone,
                                     @RequestParam(name = "otpCode")String otpCode,
                                     @RequestParam(name = "name")String name,
                                     @RequestParam(name = "gender")Integer gender,
                                     @RequestParam(name = "age")Integer age,
                                     @RequestParam(name = "password")String password) throws BusinessException {
        //验证手机号与对应的otpCode相符合
        String otpCodeInSession=(String)this.httpServletRequest.getSession().getAttribute(telephone);
        if (!com.alibaba.druid.util.StringUtils.equals(otpCode,otpCodeInSession)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"短信验证码不符合");
        }

        //用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(gender);
        userModel.setAge(age);
        userModel.setTelephone(telephone);
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(MD5Encoder.encode(password.getBytes()));

        userService.register(userModel);
        return CommonReturnType.create(null);


    }

    //用户获取otp短信接口
    @RequestMapping(value = "/getotp",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name="telephone")String telephone){
        //需要按照一定的规则生成otp验证码
        Random random = new Random();
        int randomInt=random.nextInt(99999);
        randomInt += 10000;
        String otpCode=String.valueOf(randomInt);

        //将otp验证码同对应用户的手机号关联,使用httpsession的方式绑定他的手机号和otpCode，也可以使用redis
        httpServletRequest.getSession().setAttribute(telephone,otpCode);

        //将otp验证码通过短信通道发送给用户，省略
        System.out.println("telephone="+telephone+"&otpCode="+otpCode);
        return CommonReturnType.create(null);
    }


    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name="id") Integer id) throws BusinessException {
        //调用service服务获取对应id的用户对象并返回给前端
        UserModel userModel = userService.getUserById(id);

        //当获取的用户信息不存在时
        if (userModel==null){
            //userModel.setEncrptPassword("wqwqw");
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }

        //将核心领域用户模型对象转化为可供UI使用的viewObject
        UserVO userVO=convertFromModel(userModel);
        //返回通用对象
        return CommonReturnType.create(userVO);
    }

    /**
     * 将核心领域用户模型对象转化为可供UI使用的viewObject
     * @param userModel
     * @return
     */
    private UserVO convertFromModel(UserModel userModel){
        if (userModel==null){
            return null;
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }
}

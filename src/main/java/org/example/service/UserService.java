package org.example.service;

import org.example.error.BusinessException;
import org.example.service.model.UserModel;

/**
 * @Description
 * @date 2021/4/12-22:13
 */
public interface UserService {
    //通过用户id获取用户对象方法
    UserModel getUserById(Integer id);

    //用户注册
    void register(UserModel userModel) throws BusinessException;
}

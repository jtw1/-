package org.example.service.imple;

import org.example.DataObject.UserDo;
import org.example.DataObject.UserPwdDo;
import org.example.dao.UserDoMapper;
import org.example.dao.UserPwdDoMapper;
import org.example.service.UserService;
import org.example.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Description
 * @date 2021/4/12-22:13
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserDoMapper userDoMapper;
    @Autowired
    private UserPwdDoMapper userPwdDoMapper;

    @Override
    public UserModel getUserById(Integer id) {
        //调用userDoMapper获取到对应的用户dataObject
        UserDo userDo = userDoMapper.selectByPrimaryKey(id);
        if (userDo==null){
            return null;
        }
        //通过用户id获取对应的用户加密密码信息
        UserPwdDo userPwdDo = userPwdDoMapper.selectByUserId(userDo.getId());

        return convertFromDataObject(userDo,userPwdDo);
    }

    private UserModel convertFromDataObject(UserDo userDo, UserPwdDo userPwdDo){
        if (userDo==null){
            return null;
        }
        UserModel userModel = new UserModel();
        //把userDo属性copy到userModel内
        BeanUtils.copyProperties(userDo,userModel);
        if (userPwdDo!=null){
            //这里不能再使用copy，因为userDo和userPwdDo内部有一个id字段是重复的
            userModel.setEncrptPassword(userPwdDo.getEncrptPassword());
        }

        return userModel;
    }
}

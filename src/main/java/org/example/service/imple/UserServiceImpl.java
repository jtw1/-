package org.example.service.imple;

import org.apache.commons.lang3.StringUtils;
import org.example.DataObject.UserDo;
import org.example.DataObject.UserPwdDo;
import org.example.dao.UserDoMapper;
import org.example.dao.UserPwdDoMapper;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.service.UserService;
import org.example.service.model.UserModel;
import org.example.validator.ValidationResult;
import org.example.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired
    private ValidatorImpl validator;

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

    /**
     * 用户注册 数据库层操作
     * @param userModel
     * @throws BusinessException
     */
    @Override
    @Transactional
    public void register(UserModel userModel) throws BusinessException {
        if (userModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

//        if (StringUtils.isEmpty(userModel.getName())
//                || userModel.getGender()==null
//                || userModel.getAge()==null
//                || StringUtils.isEmpty(userModel.getTelephone())){
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
//        }
        ValidationResult result = validator.validate(userModel);
        if (result.isHasErrors()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,result.getErrorMsg());
        }
        //这里使用insertSelective()而不使用insert(),是因为前者在属性赋值之前有个判空操作，如果是null就跳过，不为null则执行insert
        // 若使用insert()，当对应字段为null时，会用null字段覆盖数据库的默认值
        //这种做法对update操作尤其有利，update时，如果字段为null，就不更新

        //实现model->dataObject方法
        UserDo userDo = convertFromModel(userModel);
        try {
            userDoMapper.insertSelective(userDo);

        }catch (DuplicateKeyException e){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"手机号已重复注册");

        }

        userModel.setId(userDo.getId());
        //实现model->UserPwdDo方法
        UserPwdDo userPwdDo=convertPwdFromModel(userModel);
        userPwdDoMapper.insertSelective(userPwdDo);

        return;
    }

    /**
     * 检验用户登录是否合法
     * @param telephone
     * @param encrptPassword
     * @return
     */
    @Override
    public UserModel validateLogin(String telephone, String encrptPassword) throws BusinessException {
        //通过用户手机获取用户信息
        UserDo userDo = userDoMapper.selectByTelephone(telephone);
        if (userDo==null){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        UserPwdDo userPwdDo=userPwdDoMapper.selectByUserId(userDo.getId());
        UserModel userModel=convertFromDataObject(userDo,userPwdDo);

        //比对用户信息内加密的密码是否和传输进来的密码相匹配
        if (!StringUtils.equals(encrptPassword,userModel.getEncrptPassword())){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        return userModel;
    }


    /**
     * model->dataObject方法
     * @param userModel
     * @return
     */
    private UserDo convertFromModel(UserModel userModel){
        if (userModel==null){
            return null;
        }
        UserDo userDo = new UserDo();
        BeanUtils.copyProperties(userModel,userDo);

        return userDo;
    }

    /**
     * UserModel->UserPwdDo
     * @param userModel
     * @return
     */
    private UserPwdDo convertPwdFromModel(UserModel userModel){
        if (userModel==null){
            return null;
        }
        UserPwdDo userPwdDo = new UserPwdDo();
        userPwdDo.setEncrptPassword(userModel.getEncrptPassword());
        userPwdDo.setUserId(userModel.getId());
        return userPwdDo;
    }

    /**
     * 根据提供的userDo和userPwdDo 转化为 UserModel
     * @param userDo
     * @param userPwdDo
     * @return
     */
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

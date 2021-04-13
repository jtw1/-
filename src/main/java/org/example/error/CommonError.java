package org.example.error;

/**
 * @Description
 * @date 2021/4/13-22:38
 */
public interface CommonError {
    int getErrCode();
    String getErrMsg();
    CommonError setErrMsg(String errMsg);
}

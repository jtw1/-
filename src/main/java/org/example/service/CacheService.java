package org.example.service;

/**
 * @Description 封装本地缓存操作类
 * @date 2021/5/15-21:26
 */
public interface CacheService {
    //存方法
    void setCommonCache(String key,Object value);

    //取方法
    Object getFromCommonCache(String key);
}

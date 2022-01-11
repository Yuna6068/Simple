package com.example.blog.service;


import com.example.blog.common.base.BaseService;
import com.example.blog.entity.PostMarkRef;

/**
 * <pre>
 *     文章收藏业务逻辑接口
 * </pre>
 *
 * @author : saysky
 * @date : 2017/11/14
 */
public interface PostMarkRefService extends BaseService<PostMarkRef, Long> {


    /**
     * 根据用户ID删除
     * @param userId
     * @return
     */
    Integer deleteByUserId(Long userId);
}

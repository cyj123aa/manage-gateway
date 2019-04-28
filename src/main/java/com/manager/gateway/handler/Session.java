package com.manager.gateway.handler;


import com.hoolink.sdk.bo.base.CurrentUserBO;

import java.util.concurrent.CompletableFuture;

/**
 * 获取当前用户，主要用于登录及Token
 *
 * @author zhangxin
 * @date 2019/4/20
 */
public interface Session {

    /**
     * 提供异步获取当前用户接口
     *
     * @param token
     * @return
     */
    CompletableFuture<CurrentUserBO> getSessionUser(String token);
}

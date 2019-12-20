package com.jw.manage.gateway.handler;

import java.util.HashSet;
import java.util.Set;

/**
 * @author zhangxin
 * @date 2019/4/20
 */
public class AuthConfig {

    private static Set<String> passOperations;

    private static Set<String> passOperationsWithoutAuth;

    static {
        // 白名单列表，白名单配置规则：       微服务名 + "." + SchemaId + "." + 方法名
        passOperations = new HashSet<>();
        // 登录
        passOperations.add("/api/manage-base/web/user/login");
        // 需要登录但不需要鉴权的接口
        passOperationsWithoutAuth = new HashSet<>();
        /**
         * 个人中心部分
         */

        //获取基础信息
        passOperationsWithoutAuth.add("/manage-base/web/personalCenter/getManagerUserInfo");

    }

    public static final Set<String> getPassOperations() {
        return passOperations;
    }

    public static final Set<String> getPassOperationsWithoutAuth() {
        return passOperationsWithoutAuth;
    }
}

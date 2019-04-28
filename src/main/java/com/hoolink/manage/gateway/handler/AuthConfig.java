package com.hoolink.manage.gateway.handler;

import java.util.HashSet;
import java.util.Set;

/**
 * @author zhangxin
 * @date 2019/4/20
 */
public class AuthConfig {

    private static Set<String> passOperations;

    static {
        // 白名单列表，白名单配置规则：       微服务名 + "." + SchemaId + "." + 方法名
        passOperations = new HashSet<>();
        // 登录
        passOperations.add("manager-base.web.userController.login");
        // 退出
        passOperations.add("manager-base.web.userController.logout");
        // 获取Session，登录时鉴权用
        passOperations.add("manager-base.userController.getSessionUser");
    }

    public static final Set<String> getPassOperations() {
        return passOperations;
    }

}

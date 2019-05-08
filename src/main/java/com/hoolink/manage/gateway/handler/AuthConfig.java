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
        passOperations.add("manage-base.userController.login");
        // 获取Session，登录时鉴权用
        passOperations.add("manage-base.userController.getSessionUser");
        // 忘记密码
        passOperations.add("manage-base.userController.forgetPassword");
        // 获取验证码
        passOperations.add("manage-base.userController.getPhoneCode");
        // 绑定手机号
        passOperations.add("manage-base.userController.bindPhone");
        // 重置密码
        passOperations.add("manage-base.userController.resetPassword");


    }

    public static final Set<String> getPassOperations() {
        return passOperations;
    }

}

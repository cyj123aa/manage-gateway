package com.hoolink.manage.gateway.handler;

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
        passOperations.add("manage-base.userController.login");
        passOperations.add("manage-base.mobileUserController.login");
        passOperations.add("manage-base.roleController.getBaseMenu");
        // 获取Session，登录时鉴权用
        passOperations.add("manage-base.userController.getSessionUser");
        passOperations.add("manage-base.mobileUserController.getSessionUser");
        // 忘记密码
        passOperations.add("manage-base.userController.forgetPassword");
        passOperations.add("manage-base.mobileUserController.forgetPassword");
        // 获取验证码
        passOperations.add("manage-base.userController.getPhoneCode");
        passOperations.add("manage-base.mobileUserController.getPhoneCode");
        // 重置密码
        passOperations.add("manage-base.userController.resetPassword");
        passOperations.add("manage-base.mobileUserController.resetPassword");
        // 校验手机验证码
        passOperations.add("manage-base.userController.verifyPhoneCode");
        passOperations.add("manage-base.mobileUserController.verifyPhoneCode");

        // 文件
        passOperations.add("hoolink-ability.web.ObsController.uploadCustom");
        passOperations.add("hoolink-ability.web.ObsController.uploadManager");
        passOperations.add("manage-base.mobileUserController.logout");

        // 需要登录但不需要鉴权的接口
        passOperationsWithoutAuth = new HashSet<>();
        /**
         * 个人中心部分
         */

        //获取基础信息
        passOperationsWithoutAuth.add("manage-base.personalCenterController.getManagerUserInfo");
        //修改密码
        passOperationsWithoutAuth.add("manage-base.userController.updatePassword");
        passOperationsWithoutAuth.add("manage-base.mobileUserController.updatePassword");
        //保存头像
        passOperationsWithoutAuth.add("manage-base.personalCenterController.updateImageId");
        //绑定手机号
        passOperationsWithoutAuth.add("manage-base.userController.bindPhone");
        passOperationsWithoutAuth.add("manage-base.mobileUserController.bindPhone");
        //用户退出
        passOperationsWithoutAuth.add("manage-base.userController.logout");
        passOperationsWithoutAuth.add("manage-base.mobileUserController.logout");

        //获取基础信息
        passOperationsWithoutAuth.add("manage-base.mobile.personalCenterController.getManagerUserInfo");
        //上传并保存头像
        passOperationsWithoutAuth.add("manage-base.mobile.personalCenterController.uploadImage");
    }

    public static final Set<String> getPassOperations() {
        return passOperations;
    }

    public static final Set<String> getPassOperationsWithoutAuth() {
        return passOperationsWithoutAuth;
    }
}

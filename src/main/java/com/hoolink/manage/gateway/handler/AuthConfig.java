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
        passOperations.add("/manage-base/web/user/login");
        passOperations.add("/manage-base/mobile/user/login");
        passOperations.add("/manage-base/web/role/getBaseMenu");
        // 获取Session，登录时鉴权用
        passOperations.add("/manage-base/web/user/getSessionUser");
        passOperations.add("/manage-base/mobile/user/getSessionUser");
        // 忘记密码
        passOperations.add("/manage-base/web/user/forgetPassword");
        passOperations.add("/manage-base/mobile/user/forgetPassword");
        // 获取验证码
        passOperations.add("/manage-base/web/user/getPhoneCode");
        passOperations.add("/manage-base/web/user/bindPhoneGetCode");
        passOperations.add("/manage-base/mobile/user/getPhoneCode");
        passOperations.add("/manage-base/mobile/user/bindPhoneGetCode");
        // 重置密码
        passOperations.add("/manage-base/web/user/resetPassword");
        passOperations.add("/manage-base/mobile/user/resetPassword");
        // 校验手机验证码
        passOperations.add("/manage-base/web/user/verifyPhoneCode");
        passOperations.add("/manage-base/mobile/user/verifyPhoneCode");

        // 文件
        passOperations.add("/hoolink-ability/web/obs/uploadCustom");
        passOperations.add("/hoolink-ability/web/obs/uploadManager");

        // 需要登录但不需要鉴权的接口
        passOperationsWithoutAuth = new HashSet<>();
        /**
         * 个人中心部分
         */

        //获取基础信息
        passOperationsWithoutAuth.add("/manage-base/web/personalCenter/getManagerUserInfo");
        //修改密码
        passOperationsWithoutAuth.add("/manage-base/web/user/updatePassword");
        passOperationsWithoutAuth.add("/manage-base/mobile/user/updatePassword");
        //保存头像
        passOperationsWithoutAuth.add("/manage-base/web/personalCenter/updateImageId");
        //绑定手机号
        passOperationsWithoutAuth.add("/manage-base/web/user/bindPhone");
        passOperationsWithoutAuth.add("/manage-base/mobile/user/bindPhone");
        //用户退出
        passOperationsWithoutAuth.add("/manage-base/web/user/logout");
        passOperationsWithoutAuth.add("/manage-base/mobile/user/logout");

        //获取基础信息
        passOperationsWithoutAuth.add("/manage-base/mobile/personalCenter/getManagerUserInfo");
        //上传并保存头像
        passOperationsWithoutAuth.add("/manage-base/mobile/personalCenter/uploadImage");
    }

    public static final Set<String> getPassOperations() {
        return passOperations;
    }

    public static final Set<String> getPassOperationsWithoutAuth() {
        return passOperationsWithoutAuth;
    }
}

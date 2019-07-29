package com.hoolink.manage.gateway.handler;

import com.hoolink.sdk.bo.base.CurrentUserBO;
import com.hoolink.sdk.constants.ContextConstant;
import com.hoolink.sdk.exception.HoolinkExceptionMassageEnum;
import com.hoolink.sdk.utils.BackVOUtil;
import com.hoolink.sdk.utils.JSONUtils;
import com.hoolink.sdk.vo.BackVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.servicecomb.core.Handler;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.provider.pojo.Invoker;
import org.apache.servicecomb.swagger.invocation.AsyncResponse;
import org.apache.servicecomb.swagger.invocation.Response;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author zhangxin
 * @date 2019/4/20
 */
@Slf4j
public class AuthHandler implements Handler {

    private Session session = Invoker.createProxy("manage-base", "userController", Session.class);

    /**
     * token截取后长度
     */
    private static final int TOKEN_LENGTH = 64;


    /**
     *      * 白名单检查，此处没有使用url进行白名单过滤
     *      * 主要原因是部分后端请求也会进 handler，此时不能直接获取url对应地址
     *      * 实现参考官方文档设计
     *      https://huaweicse.github.io/cse-java-chassis-doc/featured-topics/develop-microservice-using-cse/authentication.html
     * @param invocation
     * @param asyncResponse
     * @throws Exception
     */
    @Override
    public void handle(Invocation invocation, AsyncResponse asyncResponse) throws Exception {

        if (AuthConfig.getPassOperations().contains(invocation.getOperationMeta().getMicroserviceQualifiedName())) {
            invocation.next(asyncResponse);
        }else{
            String mobileToken=invocation.getContext(ContextConstant.MOBILE_TOKEN);
            String token ;
            if(StringUtils.isNotBlank(mobileToken)){
                token=invocation.getContext(ContextConstant.MOBILE_TOKEN);
            }else{
                token = invocation.getContext(ContextConstant.TOKEN);
            }
            // 没有token或token长度不对则无权限访问
            if (token == null && mobileToken==null) {
                asyncResponse.complete(Response.succResp(
                        BackVOUtil.operateError(HoolinkExceptionMassageEnum.NOT_AUTH.getMassage())));
                return;
            }
            CompletableFuture<CurrentUserBO> userFuture;
            if(StringUtils.isNotBlank(mobileToken)){
                userFuture = session.getSessionUser(token,true);
            }else{
                if(StringUtils.isNotBlank(token)) {
                    userFuture = session.getSessionUser(token, false);
                }else{
                    userFuture = session.getSessionUser("", false);
                }
            }
            checkCurrentUser( userFuture,asyncResponse,invocation,mobileToken,token);
        }
    }

    private void checkCurrentUser(CompletableFuture<CurrentUserBO> userFuture,AsyncResponse asyncResponse,Invocation invocation,String mobileToken,String token){
        userFuture.whenComplete((currentUser, e) -> {
            if (userFuture.isCompletedExceptionally()) {
                asyncResponse.complete(Response.succResp(
                        BackVOUtil.operateError(HoolinkExceptionMassageEnum.NOT_AUTH.getMassage())));
                return;
            } else {
                // 当前用户登录超时
                if (currentUser == null) {
                    asyncResponse.complete(Response.succResp(
                            BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.USER_SESSION_EMPTY.getMassage())));
                    return;
                }
                // 异地登录
                if(StringUtils.isNotBlank(mobileToken)){
                    if (!Objects.equals(token, currentUser.getMobileToken())) {
                        asyncResponse.complete(Response.succResp(
                                BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.OTHER_USER_LOGIN.getMassage())));
                        return;
                    }
                }else {
                    if (!Objects.equals(token, currentUser.getToken())) {
                        asyncResponse.complete(Response.succResp(
                                BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.OTHER_USER_LOGIN.getMassage())));
                        return;
                    }
                }
                // 账号删除
                if (currentUser.getEnabled() != null && !currentUser.getEnabled()) {
                    asyncResponse.complete(Response.succResp(
                            BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.USER_ACCOUNT_NOT_EXIST.getMassage())));
                    return;
                }
                //角色被禁用
                if(currentUser.getRoleStatus()!=null && !currentUser.getRoleStatus()){
                    asyncResponse.complete(Response.succResp(
                            BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.ROLE_STATUS_DISABLED.getMassage())));
                    return;
                }
                // 账号禁用
                if (currentUser.getStatus() != null && !currentUser.getStatus()) {
                    asyncResponse.complete(Response.succResp(
                            BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.USER_FORBIDDEN.getMassage())));
                    return;
                }

                // 请求鉴权
                if (!AuthConfig.getPassOperationsWithoutAuth().contains(invocation.getOperationMeta().getMicroserviceQualifiedName()) && !checkAuth(invocation.getContext(ContextConstant.REQUEST_PATH), currentUser.getAccessUrlSet())) {
                    log.info("current request path: {} forbidden", invocation.getContext(ContextConstant.REQUEST_PATH));
                    asyncResponse.complete(Response.succResp(
                            BackVOUtil.operateError(HoolinkExceptionMassageEnum.NOT_AUTH.getMassage())));
                    currentUser.setAuthUrls(null);
                    //设置全局用户
                    invocation.addContext(ContextConstant.MANAGE_CURRENT_USER, JSONUtils.toJSONString(currentUser));
                    return;
                }
                currentUser.setAuthUrls(null);
                //设置全局用户
                invocation.addContext(ContextConstant.MANAGE_CURRENT_USER, JSONUtils.toJSONString(currentUser));
                log.info("CurrentUser:{},Microservice:{},SchemaID:{},OperationName:{}",
                        currentUser.getAccount(),
                        invocation.getMicroserviceName(),
                        invocation.getSchemaId(),
                        invocation.getOperationName());
                try {
                    invocation.next(asyncResponse);
                } catch (Throwable ex) {
                    log.error("servic is error :{},method:{}", invocation.getMicroserviceName(), invocation.getInvocationQualifiedName());
                    String message = invocation.getMicroserviceName() + HoolinkExceptionMassageEnum.NOT_REGISTERED_IN_THE_REGISTRY.getMassage();
                    BackVO backVO = BackVOUtil.operateError(message);
                    asyncResponse.success(backVO);
                }
            }
        });
    }

    /**
     * 鉴权检查
     */
    private static boolean checkAuth(String currentPath, Set<String> authUrls) {
        //执行鉴权操作
        return CollectionUtils.isNotEmpty(authUrls) && authUrls.contains(currentPath);
    }
}
package com.hoolink.manage.gateway.handler;

import com.hoolink.sdk.bo.base.CurrentUserBO;
import com.hoolink.sdk.constants.ContextConstant;
import com.hoolink.sdk.exception.HoolinkExceptionMassageEnum;
import com.hoolink.sdk.utils.BackVOUtil;
import com.hoolink.sdk.utils.JSONUtils;
import com.hoolink.sdk.vo.BackVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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

    @Override
    public void handle(Invocation invocation, AsyncResponse asyncResponse) throws Exception {
        /*
         * 白名单检查，此处没有使用url进行白名单过滤
         * 主要原因是部分后端请求也会进 handler，此时不能直接获取url对应地址
         * 实现参考官方文档设计
         * https://huaweicse.github.io/cse-java-chassis-doc/featured-topics/develop-microservice-using-cse/authentication.html
         */
        if (AuthConfig.getPassOperations().contains(invocation.getOperationMeta().getMicroserviceQualifiedName())) {
            invocation.next(asyncResponse);
        } else {
            // 这里实现token验证
            String token = invocation.getContext(ContextConstant.TOKEN);
            // 没有token或token长度不对则无权限访问
//            if (token == null || token.length() < TOKEN_LENGTH) {
            if (token == null) {
                asyncResponse.complete(Response.succResp(
                        BackVOUtil.operateError(HoolinkExceptionMassageEnum.NOT_AUTH.getMassage())));
                return;
            }

            CompletableFuture<CurrentUserBO> userFuture = session.getSessionUser(token);
            userFuture.whenComplete((currentUser, e) -> {
                if (userFuture.isCompletedExceptionally()) {
                    asyncResponse.complete(Response.succResp(
                            BackVOUtil.operateError(HoolinkExceptionMassageEnum.NOT_AUTH.getMassage())));
                    return;
                } else {
                    // 当前用户登录超时
                    if (currentUser == null) {
                        asyncResponse.complete(Response.succResp(
                                BackVOUtil.operateError(HoolinkExceptionMassageEnum.LOGIN_TIME_OUT.getMassage())));
                        return;
                    }
                    // 异地登录
                    if (!Objects.equals(token, currentUser.getToken())) {
                        asyncResponse.complete(Response.succResp(
                                BackVOUtil.operateError(HoolinkExceptionMassageEnum.OTHER_USER_LOGIN.getMassage())));
                        return;
                    }

                    // 请求鉴权
                    log.info("current request path:{}", invocation.getContext(ContextConstant.REQUEST_PATH));
                    if (!checkAuth(invocation.getContext(ContextConstant.REQUEST_PATH), currentUser.getAuthUrls())) {
                    	asyncResponse.complete(Response.succResp(
	                		  BackVOUtil.operateError(HoolinkExceptionMassageEnum.NOT_AUTH.getMassage())));
                    	return;
                    }
                    
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
    }

    /**
     * 鉴权检查
     */
    private static boolean checkAuth(String currentPath, Set<String> authUrls) {
        //执行鉴权操作
        return CollectionUtils.isNotEmpty(authUrls) && authUrls.contains(currentPath);
    }
}
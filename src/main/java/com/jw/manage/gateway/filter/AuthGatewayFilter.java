package com.jw.manage.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.jw.manage.gateway.constant.Constant;
import com.jw.manage.gateway.feign.SessionFeign;
import com.jw.manage.gateway.handler.AuthConfig;
import com.jw.sdk.bo.base.CurrentUserBO;
import com.jw.sdk.constants.ContextConstant;
import com.jw.sdk.exception.ExceptionMassageEnum;
import com.jw.sdk.utils.BackVOUtil;
import com.jw.sdk.utils.JSONUtils;
import com.jw.sdk.utils.UUIDUtil;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author chenyuejun
 * @date 2019/12/16 11:02
 */
@Component
@Slf4j
@Data
public class AuthGatewayFilter implements GlobalFilter, Ordered {

    @Autowired
    private SessionFeign sessionFeign;
    private static final int SESSION_TIMEOUT_SECONDS = 120;
    private static final String API = "/api";




    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange,
        GatewayFilterChain gatewayFilterChain) {
        String path = serverWebExchange.getRequest().getURI().getPath();
        //链路id，这个请求在整个请求链路中的id，用于定位问题
        String txId = UUIDUtil.getTxId();
        System.out.println("aaaaaaaaaaaaaaaaaaaaaa");
        log.info("come path "+path);
      /*  Flux<DataBuffer> cachedBody = serverWebExchange.getAttribute(Constant.CACHE_REQUEST_BODY_OBJECT_KEY);
        String body = readBody(cachedBody);
        log.info("[txId]: {} microService is: {}, url is: {}, params is: {}", txId, path.split("/")[1], path, body);
        */
        // 用户校验 和存当前用户的信息
        CurrentUserBO currentUserBO;
        //白名单放行
        if (AuthConfig.getPassOperations().contains(path)) {
            if(path.endsWith(Constant.LOGIN) || path.endsWith(Constant.LOGOUT)){
                return gatewayFilterChain.filter(serverWebExchange);
            }
            String token = serverWebExchange.getRequest().getHeaders().getFirst(ContextConstant.TOKEN);
            String mToken = serverWebExchange.getRequest().getHeaders().getFirst(ContextConstant.MOBILE_TOKEN);

            if (StringUtils.isNotBlank(token) && token.length() >= Constant.TOKEN_LENGTH) {
                //web端验证
                currentUserBO = sessionFeign.getSession(token, false);
                currentUserBO.setAccessUrlSet(null);
                currentUserBO.setAuthUrls(null);
                serverWebExchange.getRequest().mutate().header(ContextConstant.MANAGE_CURRENT_USER, JSONUtils.toJSONString(currentUserBO));
                serverWebExchange.getRequest().mutate().header(ContextConstant.TX_ID, txId);
                return gatewayFilterChain.filter(serverWebExchange);
            } else if(StringUtils.isNotBlank(mToken) && mToken.length() >= Constant.TOKEN_LENGTH) {
                   // 移动端验证
                    currentUserBO = sessionFeign.getSession(token, true);
                    //设置全局用户，清空authUrls避免请求头过大
                    currentUserBO.setAccessUrlSet(null);
                    currentUserBO.setAuthUrls(null);
                    serverWebExchange.getRequest().mutate().header(ContextConstant.MANAGE_CURRENT_USER, JSONUtils.toJSONString(currentUserBO));
                    serverWebExchange.getRequest().mutate().header(ContextConstant.TX_ID, txId);
                return gatewayFilterChain.filter(serverWebExchange);
            } else {
                setResponseOk(serverWebExchange);
                String result=JSONObject.toJSONString(BackVOUtil.operateError(ExceptionMassageEnum.NOT_AUTH.getMassage()));
                return serverWebExchange.getResponse().writeWith(Mono.just(serverWebExchange.getResponse().bufferFactory().wrap(result.getBytes())));
            }
        } else {
            //进行权限验证 获取token
            String mobileToken = serverWebExchange.getRequest().getHeaders().getFirst(ContextConstant.MOBILE_TOKEN);
            String token;
            if (StringUtils.isNotBlank(mobileToken)) {
                token = serverWebExchange.getRequest().getHeaders().getFirst(ContextConstant.MOBILE_TOKEN);
            } else {
                token = serverWebExchange.getRequest().getHeaders().getFirst(ContextConstant.TOKEN);
            }
            // 没有token或token长度不对则无权限访问
            if (StringUtils.isBlank(token)) {
                ServerHttpResponse response = serverWebExchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
            // 用户校验 和存当前用户的信息
            if (StringUtils.isNotBlank(mobileToken)) {
                currentUserBO = sessionFeign.getSession(token, true);
            } else {
                if (StringUtils.isNotBlank(token)) {
                    currentUserBO = sessionFeign.getSession(token, false);
                } else {
                    currentUserBO = sessionFeign.getSession("", false);
                }
            }
            return checkCurrentUser(currentUserBO, serverWebExchange, gatewayFilterChain, mobileToken, token, txId);
        }
    }

    private Mono<Void> checkCurrentUser(CurrentUserBO currentUser,
        ServerWebExchange exchange, GatewayFilterChain gatewayFilterChain,
        String mobileToken, String token,String txId) {

        String result;
        // 当前用户登录超时
        if (currentUser == null) {
            setResponseOk(exchange);
            result= JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(ExceptionMassageEnum.USER_SESSION_EMPTY.getMassage()));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
        }
        // 异地登录
        if(StringUtils.isNotBlank(mobileToken)){
            if (!Objects.equals(token, currentUser.getMobileToken())) {
                setResponseOk(exchange);
                result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(ExceptionMassageEnum.OTHER_USER_LOGIN.getMassage()));
                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
            }
        }else {
            if (!Objects.equals(token, currentUser.getToken())) {
                setResponseOk(exchange);
                result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(ExceptionMassageEnum.OTHER_USER_LOGIN.getMassage()));
                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
            }
        }
        // 账号删除
        if (currentUser.getEnabled() != null && !currentUser.getEnabled()) {
            setResponseOk(exchange);
            result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(ExceptionMassageEnum.USER_ACCOUNT_NOT_EXIST.getMassage()));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
        }
        //角色被禁用
        if(currentUser.getRoleStatus()!=null && !currentUser.getRoleStatus()){
            setResponseOk(exchange);
            result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(ExceptionMassageEnum.ROLE_STATUS_DISABLED.getMassage()));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
        }
        // 账号禁用
        if (currentUser.getStatus() != null && !currentUser.getStatus()) {
            setResponseOk(exchange);
            result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(ExceptionMassageEnum.USER_FORBIDDEN.getMassage()));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
        }

        // 请求鉴权   鉴权逻辑
         /*  String auth = exchange.getRequest().getURI().getPath().split(API)[1];
     // 当权限当中的url 在登录后白名单中时，或者在权限接口内   也可以在下层服务自定义
        if(!AuthConfig.getPassOperationsWithoutAuth().contains(auth) && !checkAuth(auth,currentUser.getAccessUrlSet())){
            setResponseOk(exchange);
            result=JSONObject.toJSONString(BackVOUtil.operateError(ExceptionMassageEnum.NOT_AUTH.getMassage()));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
        }*/

        currentUser.setAuthUrls(null);
        currentUser.setAccessUrlSet(null);
        //设置全局用户

        exchange.getRequest().mutate().header(ContextConstant.MANAGE_CURRENT_USER, JSONUtils.toJSONString(currentUser));
        exchange.getRequest().mutate().header(ContextConstant.TX_ID, txId);
        try {
            return gatewayFilterChain.filter(exchange);
        } catch (Throwable ex) {
            log.error("service is error :{},url:{}", ex.getMessage(), exchange.getRequest().getPath());
        }
        return null;
    }

    private void setResponseOk(ServerWebExchange exchange){
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
    }
    /**
     * 鉴权检查
     */
    private static boolean checkAuth(String currentPath, Set<String> authUrls) {
        //执行鉴权操作
        return CollectionUtils.isNotEmpty(authUrls) && authUrls.contains(currentPath);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * 获取请教的 request body
     * @param body
     * @return
     */
    private static String readBody(Flux<DataBuffer> body) {
        AtomicReference<String> rawRef = new AtomicReference<>();

        body.subscribe(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            rawRef.set(Strings.fromUTF8ByteArray(bytes));
        });
        return rawRef.get();
    }

}

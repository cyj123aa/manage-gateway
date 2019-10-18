package com.hoolink.manage.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoolink.manage.gateway.constant.Constant;
import com.hoolink.manage.gateway.feign.SessionFeign;
import com.hoolink.manage.gateway.handler.AuthConfig;
import com.hoolink.sdk.bo.base.CurrentUserBO;
import com.hoolink.sdk.constants.ContextConstant;
import com.hoolink.sdk.exception.HoolinkExceptionMassageEnum;
import com.hoolink.sdk.utils.BackVOUtil;
import com.hoolink.sdk.utils.JSONUtils;
import com.hoolink.sdk.utils.UUIDUtil;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author chenzhixiong
 * @date 2019/10/14 11:02
 */
@Component
@Slf4j
@Data
public class AuthGatewayFilter implements GlobalFilter, Ordered {

    @Autowired
    private SessionFeign sessionFeign;
    private ObjectMapper objectMapper;
    private static final int SESSION_TIMEOUT_SECONDS = 120;
    private static final String API = "/api";


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange,
        GatewayFilterChain gatewayFilterChain) {
        log.info("请求Method:{}",serverWebExchange.getRequest().getMethod().name());
        String path = serverWebExchange.getRequest().getURI().getPath();
        log.info("请求的path:{}",path);
        String url = path.split(API)[1];
        // 用户校验 和存当前用户的信息
        CurrentUserBO currentUserBO;
        String txId = UUIDUtil.getTxId();
        //白名单放行
        if (AuthConfig.getPassOperations().contains(url)) {
            if(url.endsWith(Constant.LOGIN) || url.endsWith(Constant.LOGOUT)){
                return gatewayFilterChain.filter(serverWebExchange);
            }
            String token = serverWebExchange.getRequest().getHeaders().getFirst(ContextConstant.TOKEN);
            String mToken = serverWebExchange.getRequest().getHeaders().getFirst(ContextConstant.MOBILE_TOKEN);
            if (StringUtils.isNotBlank(token) && token.length() >= Constant.TOKEN_LENGTH) {
                currentUserBO = sessionFeign.getSession(token, false);
                currentUserBO.setAccessUrlSet(null);
                currentUserBO.setAuthUrls(null);
                serverWebExchange.getRequest().mutate().header(ContextConstant.MANAGE_CURRENT_USER, JSONUtils.toJSONString(currentUserBO));
                serverWebExchange.getRequest().mutate().header(ContextConstant.TX_ID, txId);
                return gatewayFilterChain.filter(serverWebExchange);
            } else if(StringUtils.isNotBlank(mToken) && mToken.length() >= Constant.TOKEN_LENGTH) {
                    currentUserBO = sessionFeign.getSession(token, true);
                    //设置全局用户，清空authUrls避免请求头过大
                    currentUserBO.setAccessUrlSet(null);
                    currentUserBO.setAuthUrls(null);
                    serverWebExchange.getRequest().mutate().header(ContextConstant.MANAGE_CURRENT_USER, JSONUtils.toJSONString(currentUserBO));
                    serverWebExchange.getRequest().mutate().header(ContextConstant.TX_ID, txId);
                return gatewayFilterChain.filter(serverWebExchange);
            } else {
                setResponseOk(serverWebExchange);
                String result=JSONObject.toJSONString(BackVOUtil.operateError(HoolinkExceptionMassageEnum.NOT_AUTH.getMassage()));
                return serverWebExchange.getResponse().writeWith(Mono.just(serverWebExchange.getResponse().bufferFactory().wrap(result.getBytes())));
            }
        } else {
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
            return checkCurrentUser(currentUserBO, serverWebExchange, gatewayFilterChain,
                mobileToken, token, txId);
        }
    }

    private Mono<Void> checkCurrentUser(CurrentUserBO currentUser,
        ServerWebExchange exchange, GatewayFilterChain gatewayFilterChain,
        String mobileToken, String token,String txId) {

        String result;
        // 当前用户登录超时
        if (currentUser == null) {
            setResponseOk(exchange);
            result= JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.USER_SESSION_EMPTY.getMassage()));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
        }
        // 异地登录
        if(StringUtils.isNotBlank(mobileToken)){
            if (!Objects.equals(token, currentUser.getMobileToken())) {
                setResponseOk(exchange);
                result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.OTHER_USER_LOGIN.getMassage()));
                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
            }
        }else {
            if (!Objects.equals(token, currentUser.getToken())) {
                setResponseOk(exchange);
                result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.OTHER_USER_LOGIN.getMassage()));
                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
            }
        }
        // 账号删除
        if (currentUser.getEnabled() != null && !currentUser.getEnabled()) {
            setResponseOk(exchange);
            result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.USER_ACCOUNT_NOT_EXIST.getMassage()));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
        }
        //角色被禁用
        if(currentUser.getRoleStatus()!=null && !currentUser.getRoleStatus()){
            setResponseOk(exchange);
            result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.ROLE_STATUS_DISABLED.getMassage()));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
        }
        // 账号禁用
        if (currentUser.getStatus() != null && !currentUser.getStatus()) {
            setResponseOk(exchange);
            result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.USER_FORBIDDEN.getMassage()));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
        }

        // 请求鉴权
        String auth = exchange.getRequest().getURI().getPath().split(API)[1];
        if(!AuthConfig.getPassOperationsWithoutAuth().contains(auth) && !checkAuth(auth,currentUser.getAccessUrlSet())){
            setResponseOk(exchange);
            result=JSONObject.toJSONString(BackVOUtil.operateError(HoolinkExceptionMassageEnum.NOT_AUTH.getMassage()));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));

        }

        currentUser.setAuthUrls(null);
        currentUser.setAccessUrlSet(null);
        //设置全局用户
        exchange.getRequest().mutate().header(ContextConstant.MANAGE_CURRENT_USER, JSONUtils.toJSONString(currentUser));
        exchange.getRequest().mutate().header(ContextConstant.TX_ID, txId);
        log.info("CurrentUser:{},Microservice:{}", currentUser.getAccount(), exchange.getApplicationContext().getApplicationName());
        try {
            return gatewayFilterChain.filter(exchange);
        } catch (Throwable ex) {
            log.error("servic is error :{},url:{}",
                exchange.getApplicationContext().getApplicationName(), exchange.getRequest().getPath());
        }
        return null;
    }

    /**
     * 认证错误输出
     *
     * @param resp 响应对象
     * @param mess 错误信息
     */
    private Mono<Void> authErro(ServerHttpResponse resp, String mess) {
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        DataBuffer buffer = resp.bufferFactory().wrap(mess.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Flux.just(buffer));
    }

    private String getKey(Long userId) {
        return Constant.SESSION_PREFIX + userId;
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


}

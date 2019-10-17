package com.hoolink.manage.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoolink.manage.gateway.constant.Constant;
import com.hoolink.manage.gateway.feign.SessionFeign;
import com.hoolink.manage.gateway.handler.AuthConfig;
import com.hoolink.sdk.bo.base.CurrentUserBO;
import com.hoolink.sdk.constants.ContextConstant;
import com.hoolink.sdk.exception.HoolinkExceptionMassageEnum;

import com.hoolink.sdk.utils.JSONUtils;
import com.hoolink.sdk.utils.UUIDUtil;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Resource;
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
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
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


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange,
        GatewayFilterChain gatewayFilterChain) {
        String url = serverWebExchange.getRequest().getURI().getPath();
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
                return authErro(serverWebExchange.getResponse(),
                    HoolinkExceptionMassageEnum.NOT_AUTH.getMassage());
            }
        } else {
            String mobileToken = serverWebExchange.getRequest().getHeaders()
                .getFirst(ContextConstant.MOBILE_TOKEN);
            String token;
            if (StringUtils.isNotBlank(mobileToken)) {
                token = serverWebExchange.getRequest().getHeaders().getFirst(ContextConstant.MOBILE_TOKEN);
            } else {
                token = serverWebExchange.getRequest().getHeaders().getFirst(ContextConstant.TOKEN);
            }
            // 没有token或token长度不对则无权限访问
            if (StringUtils.isNotBlank(token)) {
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
        ServerWebExchange serverWebExchange, GatewayFilterChain gatewayFilterChain,
        String mobileToken, String token,String txId) {
        // 当前用户登录超时
        if (currentUser == null) {
            return authErro(serverWebExchange.getResponse(),
                HoolinkExceptionMassageEnum.USER_SESSION_EMPTY.getMassage());
        }
        // 异地登录
        if (StringUtils.isNotBlank(mobileToken)) {
            if (!Objects.equals(token, currentUser.getMobileToken())) {
                return authErro(serverWebExchange.getResponse(),
                    HoolinkExceptionMassageEnum.OTHER_USER_LOGIN.getMassage());
            }
        } else {
            if (!Objects.equals(token, currentUser.getToken())) {
                return authErro(serverWebExchange.getResponse(),
                    HoolinkExceptionMassageEnum.OTHER_USER_LOGIN.getMassage());
            }
        }
        // 账号删除
        if (currentUser.getEnabled() != null && !currentUser.getEnabled()) {
            return authErro(serverWebExchange.getResponse(),
                HoolinkExceptionMassageEnum.USER_ACCOUNT_NOT_EXIST.getMassage());
        }
        //角色被禁用
        if (currentUser.getRoleStatus() != null && !currentUser.getRoleStatus()) {
            return authErro(serverWebExchange.getResponse(),
                HoolinkExceptionMassageEnum.ROLE_STATUS_DISABLED.getMassage());
        }
        // 账号禁用
        if (currentUser.getStatus() != null && !currentUser.getStatus()) {
            return authErro(serverWebExchange.getResponse(),
                HoolinkExceptionMassageEnum.USER_FORBIDDEN.getMassage());
        }

        // 请求鉴权
        if(!checkAuth(serverWebExchange.getRequest().getURI().getPath(),currentUser.getAccessUrlSet())){
            return authErro(serverWebExchange.getResponse(),
                HoolinkExceptionMassageEnum.NOT_AUTH.getMassage());
        }

        currentUser.setAuthUrls(null);
        currentUser.setAccessUrlSet(null);
        //设置全局用户
        serverWebExchange.getRequest().mutate().header(ContextConstant.MANAGE_CURRENT_USER, JSONUtils.toJSONString(currentUser));
        serverWebExchange.getRequest().mutate().header(ContextConstant.TX_ID, txId);
        log.info("CurrentUser:{},Microservice:{}", currentUser.getAccount(),
            serverWebExchange.getApplicationContext().getApplicationName());
        try {
            return gatewayFilterChain.filter(serverWebExchange);
        } catch (Throwable ex) {
            log.error("servic is error :{},url:{}",
                serverWebExchange.getApplicationContext().getApplicationName(),
                serverWebExchange.getRequest().getPath());
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

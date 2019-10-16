//package com.hoolink.manage.gateway.filter;
//
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//
//import com.hoolink.manage.gateway.feign.SessionFeign;
//import com.hoolink.manage.gateway.handler.AuthConfig;
//import com.hoolink.sdk.bo.base.CurrentUserBO;
//import com.hoolink.sdk.constants.ContextConstant;
//import com.hoolink.sdk.exception.HoolinkExceptionMassageEnum;
//import com.hoolink.sdk.utils.BackVOUtil;
//import com.hoolink.sdk.utils.JSONUtils;
//import java.util.Objects;
//import java.util.Set;
//import javax.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.collections.CollectionUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
///**
// * @Author: xuli
// * @Date: 2019/10/11 17:17
// */
//@Slf4j
//@Component
//public class AuthFilter implements GlobalFilter, Ordered {
//
//    @Resource
//    private SessionFeign sessionFeign;
//
//    @Autowired
//    private StringRedisTemplate stringRedisTemplate;
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        log.info("request={}", JSONArray.toJSONString(exchange.getRequest()));
//        ServerHttpRequest request = exchange.getRequest();
//        String uri=request.getURI().getPath();
//        String mobileToken="";
//        String token="";
//        CurrentUserBO currentUserBO=new CurrentUserBO();
//        //不在白名单的情况下
//        if(StringUtils.isNotBlank(uri) && !AuthConfig.getPassOperations().contains(uri)){
//            mobileToken=request.getHeaders().getFirst(ContextConstant.MOBILE_TOKEN);
//            if(StringUtils.isNotBlank(mobileToken)){
//                token=mobileToken;
//            }else{
//                token = request.getHeaders().getFirst(ContextConstant.TOKEN);
//            }
//            //如果web端和手机端的token均为空的时候表示无权限
//            if(StringUtils.isAllBlank(token,mobileToken)){
//                setResponseOk(exchange);
//                String result=JSONObject.toJSONString(BackVOUtil.operateError(HoolinkExceptionMassageEnum.NOT_AUTH.getMassage()));
//                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
//            }
//            if(StringUtils.isNotBlank(mobileToken)){
//                currentUserBO=sessionFeign.getSession(token,true);
//            }else{
//                if(StringUtils.isNotBlank(token)) {
//                    currentUserBO=sessionFeign.getSession(token,false);
//                }else{
//                    currentUserBO=sessionFeign.getSession("",false);
//                }
//            }
//        }
//        return checkCurrentUser(exchange,currentUserBO,mobileToken,token,uri,chain);
//    }
//
//        private Mono<Void> checkCurrentUser(ServerWebExchange exchange,CurrentUserBO currentUser,String mobileToken,String token,String currentPath,GatewayFilterChain chain){
//                String result;
//                // 当前用户登录超时
//                if (currentUser == null) {
//                    setResponseOk(exchange);
//                    result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.USER_SESSION_EMPTY.getMassage()));
//                    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
//                }
//                // 异地登录
//                if(StringUtils.isNotBlank(mobileToken)){
//                    if (!Objects.equals(token, currentUser.getMobileToken())) {
//                        setResponseOk(exchange);
//                        result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.OTHER_USER_LOGIN.getMassage()));
//                        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
//                    }
//                }else {
//                    if (!Objects.equals(token, currentUser.getToken())) {
//                        setResponseOk(exchange);
//                        result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.OTHER_USER_LOGIN.getMassage()));
//                        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
//                    }
//                }
//                // 账号删除
//                if (currentUser.getEnabled() != null && !currentUser.getEnabled()) {
//                    setResponseOk(exchange);
//                    result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.USER_ACCOUNT_NOT_EXIST.getMassage()));
//                    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
//                }
//                //角色被禁用
//                if(currentUser.getRoleStatus()!=null && !currentUser.getRoleStatus()){
//                    setResponseOk(exchange);
//                    result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.ROLE_STATUS_DISABLED.getMassage()));
//                    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
//                }
//                // 账号禁用
//                if (currentUser.getStatus() != null && !currentUser.getStatus()) {
//                    setResponseOk(exchange);
//                    result=JSONObject.toJSONString(BackVOUtil.operateErrorToLogin(HoolinkExceptionMassageEnum.USER_FORBIDDEN.getMassage()));
//                    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
//                }
//
//                // 请求鉴权
////                if(!checkAuth(currentPath,currentUser.getAccessUrlSet())){
//                if(false){
//                    setResponseOk(exchange);
//                    result=JSONObject.toJSONString(BackVOUtil.operateError(HoolinkExceptionMassageEnum.NOT_AUTH.getMassage()));
//                    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.getBytes())));
//                }else{
//                    currentUser.setAuthUrls(null);
//                    currentUser.setAccessUrlSet(null);
//                    //设置全局用户
//                    stringRedisTemplate.opsForValue().set(ContextConstant.MANAGE_CURRENT_USER+currentUser.getUserId(),JSONUtils.toJSONString(currentUser));
//                }
//            ServerHttpRequest request=exchange.getRequest().mutate().header(ContextConstant.MANAGE_CURRENT_USER,JSONUtils.toJSONString(currentUser)).build();
//            return chain.filter(exchange.mutate().request(request).build());
//    }
//
//    /**
//     * 鉴权检查
//     */
//    private static boolean checkAuth(String currentPath, Set<String> authUrls) {
//        //执行鉴权操作
//        return CollectionUtils.isNotEmpty(authUrls) && authUrls.contains(currentPath);
//    }
//
//    private void setResponseOk(ServerWebExchange exchange){
//        exchange.getResponse().setStatusCode(HttpStatus.OK);
//        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
//    }
//
//    @Override
//    public int getOrder() {
//        return 1;
//    }
//
//}

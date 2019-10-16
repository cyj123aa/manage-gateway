package com.hoolink.manage.gateway.filter;

import com.hoolink.sdk.constants.ContextConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * @Author: xuli
 * @Date: 2019/10/12 13:16
 */
@Component
@Slf4j
public class RequestFilter implements GlobalFilter, Ordered {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request=exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        if (request.getMethod().name().equalsIgnoreCase("OPTIONS")){
            response.getHeaders().add("Access-Control-Allow-Origin", "*");
            response.getHeaders().add("Access-Control-Allow-Methods", "*");
            response.getHeaders().add("Access-Control-Allow-Headers", "*");
        }
        String xToken = request.getHeaders().getFirst(ContextConstant.TOKEN);
        String mToken = request.getHeaders().getFirst(ContextConstant.MOBILE_TOKEN);
        if(StringUtils.isNotBlank(xToken)){
            stringRedisTemplate.opsForValue().set(ContextConstant.TOKEN,xToken);
        }
        if(StringUtils.isNotBlank(mToken)){
            stringRedisTemplate.opsForValue().set(ContextConstant.MOBILE_TOKEN,mToken);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}

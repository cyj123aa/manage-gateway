package com.hoolink.manage.gateway.dispatcher;

import com.hoolink.sdk.constants.ContextConstant;
import com.hoolink.sdk.utils.UUIDUtil;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CookieHandler;
import org.apache.servicecomb.edge.core.AbstractEdgeDispatcher;
import org.apache.servicecomb.edge.core.EdgeInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author dushuai
 * @date 2018/1/17
 */
public class ApiDispatcher extends AbstractEdgeDispatcher {
    private static Logger logger = LoggerFactory.getLogger(ApiDispatcher.class);
    private static final String API = "/api";

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void init(Router router) {
        // api + 微服务名 + 实际路径
        String regex = API + "/([^\\\\/]+)/(.*)";
        router.routeWithRegex(regex).handler(CookieHandler.create());
        router.routeWithRegex(regex).handler(createBodyHandler());
        router.routeWithRegex(regex).failureHandler(this::onFailure).handler(this::onRequest);
    }

    private void onRequest(RoutingContext context) {
        Map<String, String> pathParams = context.pathParams();
        String microserviceName = pathParams.get("param0");
        String path = "/" + pathParams.get("param1");
        /*
         * 浏览器合法验证，如果不合法，
         * 设置为合法Access-Control-Allow-Headers，返回合法Access-Control-Allow-Headers到浏览器
         */
        if (context.request().method() == HttpMethod.OPTIONS) {
            context.response().putHeader("Access-Control-Allow-Origin", "*");
            context.response().putHeader("Access-Control-Allow-Methods", "*");
            context.response().putHeader("Access-Control-Allow-Headers",
                    context.request().getHeader("Access-Control-Request-Headers"));
            context.response().end();
            return;
        }

        String txId = UUIDUtil.getTxId();
        EdgeInvocation edgeInvocation = new EdgeInvocation() {
            @Override
            protected void createInvocation() {
                super.createInvocation();
                HttpServerRequest request = context.request();
                String xToken = request.getHeader(ContextConstant.TOKEN);
                String mToken = request.getHeader(ContextConstant.MOBILE_TOKEN);
                String deviceType = request.getHeader("X-Mobile");
                String authPath = request.path().substring(request.path().indexOf('/', 1));
                invocation.addContext(ContextConstant.REQUEST_PATH, authPath);
                //token全局化
                invocation.addContext(ContextConstant.TOKEN, xToken);
                invocation.addContext(ContextConstant.MOBILE_TOKEN, mToken);
                invocation.addContext(ContextConstant.TX_ID, txId);
                invocation.addContext("X-Mobile", deviceType);
            }
        };
        context.response().putHeader("Access-Control-Allow-Origin", "*");
        context.response().putHeader("Access-Control-Allow-Methods", "*");
        //请求默认不发送Cookie和HTTP认证信息。如果要把Cookie发到服务器，一方面要服务器同意，指定Access-Control-Allow-Credentials字段
        context.response().putHeader("Access-Control-Allow-Headers", "*");
        //微服务路由转发
        String queryParams = context.getBodyAsString();
        logger.info("[txId]: {} microservice is: {}, method is: {}, params is: {}", txId, microserviceName, path, queryParams);
        edgeInvocation.init(microserviceName, context, path, httpServerFilters);
        edgeInvocation.edgeInvoke();
    }
}

package com.manager.gateway.filter;

import com.hoolink.sdk.constants.ContextConstant;
import org.apache.servicecomb.common.rest.filter.HttpServerFilter;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.foundation.vertx.http.HttpServletRequestEx;
import org.apache.servicecomb.foundation.vertx.http.HttpServletResponseEx;
import org.apache.servicecomb.swagger.invocation.Response;

/**
 * TxId 过滤器，用于响应头添加TxId信息
 *
 * @author zhangxin
 * @date 2018/7/10
 */
public class TxIdFilter implements HttpServerFilter {

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public Response afterReceiveRequest(Invocation invocation, HttpServletRequestEx requestEx) {
        return null;
    }

    @Override
    public void beforeSendResponse(Invocation invocation, HttpServletResponseEx responseEx) {
        responseEx.setHeader("TxId", invocation.getContext(ContextConstant.TX_ID));
    }
}

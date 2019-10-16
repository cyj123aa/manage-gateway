//package com.hoolink.manage.gateway.filter;
//
//import org.apache.servicecomb.common.rest.filter.HttpServerFilter;
//import org.apache.servicecomb.core.Invocation;
//import org.apache.servicecomb.foundation.vertx.http.HttpServletRequestEx;
//import org.apache.servicecomb.foundation.vertx.http.HttpServletResponseEx;
//import org.apache.servicecomb.foundation.vertx.http.VertxServerRequestToHttpServletRequest;
//import org.apache.servicecomb.swagger.invocation.Response;
//
//import javax.servlet.http.HttpServletResponse;
//
///**
// * 用户操作日志过滤器
// *
// * @author zhangxin
// * @date 2018/7/10
// */
//public class UserLogFilter implements HttpServerFilter {
//
//    private static final String USER_LOG_CONTROLLER = "UserLogController";
//    private static final String CREATE_USER_LOG = "createUserLog";
//    private static final String LOGIN_CONTROLLER = "loginController";
//    private static final String LOGIN = "login";
//    private static final String EXIT_LOGIN = "exitLogin";
//    private static final String BASE = "base";
//
//    @Override
//    public int getOrder() {
//        return 0;
//    }
//
//    @Override
//    public Response afterReceiveRequest(Invocation invocation, HttpServletRequestEx requestEx) {
//        return null;
//    }
//
//    @Override
//    public void beforeSendResponse(Invocation invocation, HttpServletResponseEx responseEx) {
//        if (invocation == null || invocation.getMicroserviceName() == null || invocation.getOperationMeta() == null) {
//            return;
//        }
//        //例外掉记录日志检查,防止登录执行授权
//        if (invocation.getMicroserviceName().contains(BASE)
//                && USER_LOG_CONTROLLER.equals(invocation.getSchemaId())
//                && CREATE_USER_LOG.equals(invocation.getOperationMeta().getOperationId())) {
//            return;
//        }
//        if (responseEx.getStatus() == HttpServletResponse.SC_OK) {
//            VertxServerRequestToHttpServletRequest request = (VertxServerRequestToHttpServletRequest) invocation.getHandlerContext().get("servicecomb-rest-request");
//            if (request != null) {
//            }
//        }
//    }
//}

package com.jw.manage.gateway.feign.fallback;

import com.jw.manage.gateway.feign.SessionFeign;
import com.jw.sdk.bo.base.CurrentUserBO;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chenyuejun
 * @date 2019/12/17 15:39
 */
@Slf4j
public class SessionFeignFallBack implements SessionFeign {

    @Override
    public CurrentUserBO getSession(String token, boolean isMobile) {
        log.info(".........方法调用异常");
        return null;
    }
}

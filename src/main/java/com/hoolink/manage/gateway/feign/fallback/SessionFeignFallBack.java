package com.hoolink.manage.gateway.feign.fallback;

import com.hoolink.manage.gateway.feign.SessionFeign;
import com.hoolink.sdk.bo.base.CurrentUserBO;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chenzhixiong
 * @date 2019/10/18 15:39
 */
@Slf4j
public class SessionFeignFallBack implements SessionFeign {

    @Override
    public CurrentUserBO getSession(String token, boolean isMobile) {
        log.info(".........方法调用异常");
        return null;
    }
}

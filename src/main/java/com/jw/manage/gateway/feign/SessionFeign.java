package com.jw.manage.gateway.feign;


import com.jw.manage.gateway.feign.fallback.SessionFeignFallBack;
import com.jw.sdk.bo.base.CurrentUserBO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author chenyuejun
 * @date 2019/10/14 16:46
 */
@FeignClient(value = "manage-base" ,fallback = SessionFeignFallBack.class )
@Component
public interface SessionFeign {
    @RequestMapping(value = "/web/user/getSessionUser",method= RequestMethod.POST)
    CurrentUserBO getSession(@RequestBody String token,@RequestParam("isMobile") boolean isMobile);
}

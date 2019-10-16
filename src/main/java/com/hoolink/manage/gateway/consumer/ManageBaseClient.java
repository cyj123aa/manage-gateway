package com.hoolink.manage.gateway.consumer;

import com.hoolink.sdk.bo.base.CurrentUserBO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

/**
 * @Author: xuli
 * @Date: 2019/10/14 14:02
 */
@FeignClient(name = "manage-base")
public interface ManageBaseClient {

     @PostMapping(value = "/web/user/getSessionUser")
     CurrentUserBO getSessionUser(@RequestBody String token, @RequestParam("isMobile")boolean isMobile);
}

package com.hoolink.manage.gateway.dispatcher;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzhixiong
 * @date 2019/10/14 13:41
 */
@Slf4j
@RestController
public class IndexController {
    /**
     * 配置了默认降级方法 对于各个服务 各个方法可以单独处理自己的降级业务
     * @return
     */
    @RequestMapping("/test")
    public Map<String,String> defaultfallback(){
        log.info(">>>>>>>>>降级操作...");
        Map<String,String> map = new HashMap<>(16);
        map.put("resultCode","fail");
        map.put("resultMessage","服务异常");
        map.put("resultObj","当前服务不可用，请稍后重试！！");
        return map;
    }

}

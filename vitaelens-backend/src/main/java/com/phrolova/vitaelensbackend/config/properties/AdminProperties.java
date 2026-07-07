package com.phrolova.vitaelensbackend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "system.admin")  //自动读取配置文件
public class AdminProperties {

    /**
     * 默认管理员用户名
     */
    private String username;

    /**
     * 默认管理员密码（明文，仅初始化时使用）
     */
    private String password;

}

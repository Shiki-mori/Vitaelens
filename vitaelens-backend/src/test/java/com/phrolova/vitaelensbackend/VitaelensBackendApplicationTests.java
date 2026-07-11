package com.phrolova.vitaelensbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
// 在运行该测试时，激活 `test` 配置文件 `application-test.yml`。其他默认配置将使用`application.yml`
@ActiveProfiles("test")
class VitaelensBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}

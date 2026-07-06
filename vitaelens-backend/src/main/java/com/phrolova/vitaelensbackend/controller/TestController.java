package com.phrolova.vitaelensbackend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@CrossOrigin
public class TestController {

    @GetMapping("/hello")  //映射HTTP GET请求到/hello路径
    // 创建一个hello方法，返回值类型为String
    public String hello() {
        return "Hello Spring Boot!";
    }

}

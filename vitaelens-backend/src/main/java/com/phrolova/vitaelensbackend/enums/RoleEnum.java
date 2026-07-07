package com.phrolova.vitaelensbackend.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
//import com.fasterxml.jackson.annotation.JsonValue;

@Getter
@AllArgsConstructor
public enum RoleEnum {

    USER("user", "普通用户"),
    ADMIN("admin", "管理员");

    @EnumValue  // 枚举值映射到数据库存储的值。数据库将存储`user`或`admin`而不是`USER`或`ADMIN`
    private final String code;

//    @JsonValue  // 序列化为JSON时，仅输出该字段（desc）。如RoleEnum.USER，JSON将返回{"code":"user","desc":"普通用户"}
    private final String desc;

//    // 该构造函数在使用@AllArgsConstructor后无必要
//    RoleEnum(String code, String desc) {
//        this.code = code;
//        this.desc = desc;
//    }
}

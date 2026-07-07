package com.phrolova.vitaelensbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.phrolova.vitaelensbackend.enums.RoleEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    private String email;

    @TableField("role")  // 指定实体类字段与数据库表字段之间的映射关系。此处二者字段名相同，实际上不需要该注解。
    private RoleEnum role;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}

package com.phrolova.vitaelensbackend.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.phrolova.vitaelensbackend.config.properties.AdminProperties;
import com.phrolova.vitaelensbackend.entity.User;
import com.phrolova.vitaelensbackend.enums.RoleEnum;
import com.phrolova.vitaelensbackend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component  // 将该类声明为Spring Bean
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final AdminProperties adminProperties;

    @Override
    public void run(String... args){

        // 查询管理员是否存在
        User existAdmin = userMapper.selectOne(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, adminProperties.getUsername())
        );

        if (existAdmin != null) {
            return;
        }

        User admin = new User();

        admin.setUsername(adminProperties.getUsername());

        admin.setPasswordHash(
                passwordEncoder.encode(adminProperties.getPassword())
        );

        admin.setRole(RoleEnum.ADMIN);

        userMapper.insert(admin);

        System.out.println("管理员账号初始化成功!");
    }

}

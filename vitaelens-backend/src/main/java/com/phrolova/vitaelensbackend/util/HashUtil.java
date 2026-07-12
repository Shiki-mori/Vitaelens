package com.phrolova.vitaelensbackend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    /**
     * MD5 将输入的任意字符串转换为 32 位的 MD5 值（16进制哈希值）
     * @param input 任意字符串
     * @return 32 位的 MD5 值
     */
    public static String md5(String input) {
        try {
            // 获取 MD5 消息摘要算法实例
            MessageDigest md = MessageDigest.getInstance("MD5");
            /*
              input.getBytes(StandardCharsets.UTF_8)：将输入字符串转换为 UTF-8 编码的字节数组
              md.digest()：对字节数组进行 MD5 计算，返回 16 字节（128位）的哈希结果
             */
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            /*
              将 16 字节（128位）的哈希结果转换为 32 位的十六进制字符串
              String.format("%02x", b)：将字节 b 格式化为 2 位十六进制字符串
             */
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 算法不可用", e);
        }
    }
}

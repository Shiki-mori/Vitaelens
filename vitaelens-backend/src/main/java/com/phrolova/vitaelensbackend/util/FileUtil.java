package com.phrolova.vitaelensbackend.util;

import java.util.Set;
import java.util.UUID;

public class FileUtil {

    // Set.of：快速创建不可变的Set集合。不可修改。
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf","docx");

    // 获取文件扩展名
    public static String getExtension(String fileName) {

        // 文件名不能为空
        if (fileName == null || fileName.isEmpty()){
            return "";
        }

        // 获取最后一个点号的索引
        int dotIndex = fileName.lastIndexOf(".");

        // 处理隐藏文件（如.gitignore）等特殊情况
        if (dotIndex <= 0) {
            return "";
        }

        // 获取扩展名并转为小写
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    // 生成新的唯一文件名
    public static String generateFileName(String originalFileName) {

        String extension = getExtension(originalFileName);
        if(!ALLOWED_EXTENSIONS.contains(extension)){
            throw new IllegalArgumentException("Invalid file extension: " + extension);
        }

        // 生成随机文件名，防止上传文件重名覆盖
        return UUID.randomUUID() + "." + extension;
    }

}

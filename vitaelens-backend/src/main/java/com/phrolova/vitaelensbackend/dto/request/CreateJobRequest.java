package com.phrolova.vitaelensbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateJobRequest {

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 50, message = "岗位名称不能超过50个字符")
    private String title;

    @NotBlank(message = "岗位描述不能为空")
    @Size(max = 5000, message = "岗位描述不能超过5000个字符")
    private String content;
}

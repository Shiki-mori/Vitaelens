package com.phrolova.vitaelensbackend.service;

import com.phrolova.vitaelensbackend.dto.response.ResumeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {
    ResumeResponse uploadResume(MultipartFile file, Long userId);
    List<ResumeResponse> listResumes(Long userId);
    void deleteResume(Long resumeId, Long userId);
}

package com.phrolova.vitaelensbackend.controller;

import com.phrolova.vitaelensbackend.auth.JwtFilter;
import com.phrolova.vitaelensbackend.auth.JwtUtil;
import com.phrolova.vitaelensbackend.auth.UserContext;
import com.phrolova.vitaelensbackend.common.ErrorCode;
import com.phrolova.vitaelensbackend.dto.response.ResumeResponse;
import com.phrolova.vitaelensbackend.exception.BizException;
import com.phrolova.vitaelensbackend.exception.GlobalExceptionHandler;
import com.phrolova.vitaelensbackend.service.ResumeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResumeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumeService resumeService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(1L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void upload_success() throws Exception {
        ResumeResponse response = buildResponse(1L, "resume.pdf");
        when(resumeService.uploadResume(any(), eq(1L))).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "pdf-content".getBytes());

        mockMvc.perform(multipart("/api/resumes/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fileName").value("resume.pdf"));

        verify(resumeService).uploadResume(any(), eq(1L));
    }

    @Test
    void upload_bizException_returnsErrorCode() throws Exception {
        when(resumeService.uploadResume(any(), eq(1L)))
                .thenThrow(new BizException(ErrorCode.PARAM_ERROR, "文件不能为空"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/resumes/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.PARAM_ERROR.getMessage()));
    }

    @Test
    void upload_fileError_returnsFileErrorCode() throws Exception {
        when(resumeService.uploadResume(any(), eq(1L)))
                .thenThrow(new BizException(ErrorCode.FILE_ERROR, "文件解析失败"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.pdf", "application/pdf", "bad".getBytes());

        mockMvc.perform(multipart("/api/resumes/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.FILE_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.FILE_ERROR.getMessage()));
    }

    @Test
    void upload_serviceException_returnsSystemError() throws Exception {
        when(resumeService.uploadResume(any(), eq(1L)))
                .thenThrow(new RuntimeException("unexpected"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/resumes/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SYSTEM_ERROR.getCode()));
    }

    @Test
    void upload_missingFileParameter() throws Exception {
        mockMvc.perform(multipart("/api/resumes/upload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SYSTEM_ERROR.getCode()));
    }

    @Test
    void list_success() throws Exception {
        ResumeResponse r1 = buildResponse(1L, "a.pdf");
        ResumeResponse r2 = buildResponse(2L, "b.pdf");
        when(resumeService.listResumes(1L)).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].fileName").value("a.pdf"));
    }

    @Test
    void delete_success() throws Exception {
        doNothing().when(resumeService).deleteResume(10L, 1L);

        mockMvc.perform(delete("/api/resumes/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(resumeService).deleteResume(10L, 1L);
    }

    @Test
    void delete_notFound() throws Exception {
        doThrow(new BizException(ErrorCode.NOT_FOUND, "简历不存在"))
                .when(resumeService).deleteResume(999L, 1L);

        mockMvc.perform(delete("/api/resumes/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.NOT_FOUND.getMessage()));
    }

    private ResumeResponse buildResponse(Long id, String fileName) {
        ResumeResponse response = new ResumeResponse();
        response.setId(id);
        response.setFileName(fileName);
        response.setParsedText("parsed text");
        response.setTextLength(11);
        response.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return response;
    }
}

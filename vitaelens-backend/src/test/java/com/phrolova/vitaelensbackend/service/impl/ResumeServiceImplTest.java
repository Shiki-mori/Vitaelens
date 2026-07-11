package com.phrolova.vitaelensbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.phrolova.vitaelensbackend.common.ErrorCode;
import com.phrolova.vitaelensbackend.config.UploadConfig;
import com.phrolova.vitaelensbackend.dto.response.ResumeResponse;
import com.phrolova.vitaelensbackend.entity.Resume;
import com.phrolova.vitaelensbackend.exception.BizException;
import com.phrolova.vitaelensbackend.mapper.ResumeMapper;
import com.phrolova.vitaelensbackend.support.ResumeTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeMapper resumeMapper;

    private ResumeServiceImpl resumeService;

    private UploadConfig uploadConfig;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        uploadConfig = new UploadConfig();
        uploadConfig.setUploadDir(tempDir.toString());
        resumeService = new ResumeServiceImpl(resumeMapper, uploadConfig);
    }

    @Test
    void uploadResume_pdf_success() throws IOException {
        MockMultipartFile file = ResumeTestFixtures.pdfFile();
        doAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            resume.setId(100L);
            resume.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(resumeMapper).insert(any(Resume.class));

        ResumeResponse response = resumeService.uploadResume(file, ResumeTestFixtures.DEFAULT_USER_ID);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("resume.pdf", response.getFileName());
        assertNotNull(response.getParsedText());
        assertTrue(response.getTextLength() > 0);

        ArgumentCaptor<Resume> captor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeMapper).insert(captor.capture());
        Resume saved = captor.getValue();
        assertEquals(ResumeTestFixtures.DEFAULT_USER_ID, saved.getUserId());
        assertTrue(Files.exists(Path.of(saved.getFilePath())));
    }

    @Test
    void uploadResume_docx_success() throws IOException {
        MockMultipartFile file = ResumeTestFixtures.docxFile();
        doAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            resume.setId(101L);
            resume.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(resumeMapper).insert(any(Resume.class));

        ResumeResponse response = resumeService.uploadResume(file, ResumeTestFixtures.DEFAULT_USER_ID);

        assertEquals(101L, response.getId());
        assertEquals("resume.docx", response.getFileName());
        assertTrue(response.getParsedText().contains("张三"));
    }

    @Test
    void uploadResume_emptyFile_throwsBizException() {
        MockMultipartFile file = ResumeTestFixtures.emptyFile();

        BizException ex = assertThrows(BizException.class,
                () -> resumeService.uploadResume(file, ResumeTestFixtures.DEFAULT_USER_ID));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(resumeMapper, never()).insert(any(Resume.class));
    }

    @Test
    void uploadResume_oversizedFile_throwsBizException() {
        MockMultipartFile file = ResumeTestFixtures.oversizedPdfFile();

        BizException ex = assertThrows(BizException.class,
                () -> resumeService.uploadResume(file, ResumeTestFixtures.DEFAULT_USER_ID));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(resumeMapper, never()).insert(any(Resume.class));
    }

    @Test
    void uploadResume_invalidContentType_throwsBizException() {
        MockMultipartFile file = ResumeTestFixtures.invalidContentTypeFile();

        BizException ex = assertThrows(BizException.class,
                () -> resumeService.uploadResume(file, ResumeTestFixtures.DEFAULT_USER_ID));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void uploadResume_invalidExtension_throwsBizException() {
        MockMultipartFile file = ResumeTestFixtures.invalidExtensionPdfContentType();

        BizException ex = assertThrows(BizException.class,
                () -> resumeService.uploadResume(file, ResumeTestFixtures.DEFAULT_USER_ID));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void uploadResume_corruptPdf_throwsBizException() {
        MockMultipartFile file = ResumeTestFixtures.corruptPdfFile();

        BizException ex = assertThrows(BizException.class,
                () -> resumeService.uploadResume(file, ResumeTestFixtures.DEFAULT_USER_ID));

        assertEquals(ErrorCode.FILE_ERROR, ex.getErrorCode());
        verify(resumeMapper, never()).insert(any(Resume.class));
    }

    @Test
    void uploadResume_databaseInsertFails_propagatesException() throws IOException {
        MockMultipartFile file = ResumeTestFixtures.pdfFile();
        doThrow(new RuntimeException("DB connection failed")).when(resumeMapper).insert(any(Resume.class));

        assertThrows(RuntimeException.class,
                () -> resumeService.uploadResume(file, ResumeTestFixtures.DEFAULT_USER_ID));
    }

    @Test
    void uploadResume_duplicateUpload_bothSucceed() throws IOException {
        MockMultipartFile file1 = ResumeTestFixtures.pdfFile();
        MockMultipartFile file2 = ResumeTestFixtures.pdfFile();
        doAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            resume.setId(System.nanoTime());
            resume.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(resumeMapper).insert(any(Resume.class));

        ResumeResponse first = resumeService.uploadResume(file1, ResumeTestFixtures.DEFAULT_USER_ID);
        ResumeResponse second = resumeService.uploadResume(file2, ResumeTestFixtures.DEFAULT_USER_ID);

        assertNotEquals(first.getId(), second.getId());
        verify(resumeMapper, times(2)).insert(any(Resume.class));
    }

    @Test
    void listResumes_returnsOrderedList() {
        Resume r1 = buildResume(1L, "a.pdf", LocalDateTime.now().minusHours(1));
        Resume r2 = buildResume(2L, "b.pdf", LocalDateTime.now());
        when(resumeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(r2, r1));

        List<ResumeResponse> responses = resumeService.listResumes(ResumeTestFixtures.DEFAULT_USER_ID);

        assertEquals(2, responses.size());
        assertEquals("b.pdf", responses.get(0).getFileName());
    }

    @Test
    void listResumes_nullUserId_returnsEmpty() {
        List<ResumeResponse> responses = resumeService.listResumes(null);

        assertTrue(responses.isEmpty());
        verify(resumeMapper, never()).selectList(any());
    }

    @Test
    void listResumes_noRecords_returnsEmpty() {
        when(resumeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<ResumeResponse> responses = resumeService.listResumes(999L);

        assertTrue(responses.isEmpty());
    }

    @Test
    void deleteResume_success() throws IOException {
        Path savedFile = Path.of(uploadConfig.getUploadDir(), "to-delete.pdf");
        Files.createDirectories(savedFile.getParent());
        Files.writeString(savedFile, "content");

        Resume resume = buildResume(10L, "to-delete.pdf", LocalDateTime.now());
        resume.setFilePath(savedFile.toString());
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume);
        when(resumeMapper.deleteById(10L)).thenReturn(1);

        assertDoesNotThrow(() -> resumeService.deleteResume(10L, ResumeTestFixtures.DEFAULT_USER_ID));

        verify(resumeMapper).deleteById(10L);
        assertFalse(Files.exists(savedFile));
    }

    @Test
    void deleteResume_notFound_throwsBizException() {
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> resumeService.deleteResume(999L, ResumeTestFixtures.DEFAULT_USER_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(resumeMapper, never()).deleteById(anyLong());
    }

    private Resume buildResume(Long id, String fileName, LocalDateTime createdAt) {
        Resume resume = new Resume();
        resume.setId(id);
        resume.setUserId(ResumeTestFixtures.DEFAULT_USER_ID);
        resume.setFileName(fileName);
        resume.setFilePath("/tmp/" + fileName);
        resume.setParsedText("parsed");
        resume.setCreatedAt(createdAt);
        return resume;
    }
}

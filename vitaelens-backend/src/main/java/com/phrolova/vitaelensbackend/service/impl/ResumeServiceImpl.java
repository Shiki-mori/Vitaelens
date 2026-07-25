package com.phrolova.vitaelensbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.phrolova.vitaelensbackend.common.ErrorCode;
import com.phrolova.vitaelensbackend.config.UploadConfig;
import com.phrolova.vitaelensbackend.dto.response.ResumeResponse;
import com.phrolova.vitaelensbackend.entity.Resume;
import com.phrolova.vitaelensbackend.exception.BizException;
import com.phrolova.vitaelensbackend.mapper.ResumeMapper;
import com.phrolova.vitaelensbackend.service.ResumeService;

import com.phrolova.vitaelensbackend.util.DocxParser;
import com.phrolova.vitaelensbackend.util.FileUtil;
import com.phrolova.vitaelensbackend.util.PdfParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeMapper resumeMapper;
    private final UploadConfig uploadConfig;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;  // 10MB

    @Override
    public ResumeResponse uploadResume(MultipartFile file, Long userId) {

        // Check file size
        if (file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件大小不能超过10MB");
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件类型错误。仅支持 PDF 和 DOCX 格式");
        }

        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf") && !originalFilename.toLowerCase().endsWith(".docx")) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件名必须以 .pdf 或 .docx 结尾");
        }

        // Generate filename and Save file
        String savedName = FileUtil.generateFileName(originalFilename);
        Path savePath = Paths.get(uploadConfig.getUploadDir(), savedName).toAbsolutePath().normalize();
        try {
            Files.createDirectories(savePath.getParent());
            file.transferTo(savePath);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BizException(ErrorCode.FILE_ERROR, "文件保存失败");
        }

        // 解析文本
        String parsedText;
        String extension = FileUtil.getExtension(savedName);
        try {
            File savedFile = savePath.toFile();
            parsedText = switch (extension) {
                case "pdf" -> PdfParser.extractText(savedFile);
                case "docx" -> DocxParser.extractText(savedFile);
                default -> throw new BizException(ErrorCode.FILE_ERROR, "不支持的文件格式");
            };
        }catch (IOException e) {
            log.error("文件解析失败", e);
            throw new BizException(ErrorCode.FILE_ERROR, "文件解析失败，请检查文件内容");
        }

        if (parsedText.isEmpty()) {
            throw new BizException(ErrorCode.FILE_ERROR, "无法从文件中提取文本内容，请确认文件不是纯图片");
        }

        // 存入数据库
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(originalFilename);
        resume.setFilePath(savePath.toString());
        resume.setParsedText(parsedText);
        resumeMapper.insert(resume);

        log.info("简历上传成功：userId={}, resumeId={}, 文本长度={}", userId, resume.getId(), parsedText.length());

        return toResponse(resume);
    }

    /*
     * 根据用户ID获取其所有简历，按时间顺序倒序返回
     */
    @Override
    public List<ResumeResponse> listResumes(Long userId) {

        if (userId == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        /*
         * 根据用户ID查询
         * 等值条件 WHERE user_id = #{userId}
         * 排序条件 ORDER BY created_at DESC
         */
        wrapper.eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getCreatedAt);

        // 执行查询
        List<Resume> resumes = resumeMapper.selectList(wrapper);

        // 将查询结果实体列表转换为响应对象列表
        return resumes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /*
     * 删除简历
     */
    @Override
    public void deleteResume(Long resumeId, Long userId) {

        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getId, resumeId)
                .eq(Resume::getUserId, userId);

        Resume resume = resumeMapper.selectOne(wrapper);

        // 检查简历是否存在
        if (resume == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "简历不存在");
        }

        // 逻辑删除
        resumeMapper.deleteById(resumeId);

        // 删除物理文件
        try {
            Files.deleteIfExists(Paths.get(resume.getFilePath()));
        } catch (IOException e) {
            log.warn("文件删除失败: {}", resume.getFilePath(), e);
        }

        log.info("简历删除成功：userId={}, resumeId={}", userId, resumeId);
    }

    /*
     * 将实体转换为响应对象
     */
    private ResumeResponse toResponse(Resume resume) {
        ResumeResponse response = new ResumeResponse();
        BeanUtils.copyProperties(resume, response);
        response.setTextLength(resume.getParsedText() != null ?
                resume.getParsedText().length() : 0);
        return response;
    }

}

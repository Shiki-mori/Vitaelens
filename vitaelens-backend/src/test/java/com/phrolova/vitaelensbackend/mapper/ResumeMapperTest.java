package com.phrolova.vitaelensbackend.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.phrolova.vitaelensbackend.entity.Resume;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResumeMapperTest {

    @Autowired
    private ResumeMapper resumeMapper;

    @Test
    void insertAndSelectById() {
        Resume resume = buildResume(1L, "test.pdf", "/tmp/test.pdf", "parsed content");

        resumeMapper.insert(resume);

        assertNotNull(resume.getId());
        Resume found = resumeMapper.selectById(resume.getId());
        assertNotNull(found);
        assertEquals("test.pdf", found.getFileName());
        assertEquals("parsed content", found.getParsedText());
    }

    @Test
    void selectListByUserId() {
        resumeMapper.insert(buildResume(1L, "a.pdf", "/tmp/a.pdf", "text a"));
        resumeMapper.insert(buildResume(1L, "b.pdf", "/tmp/b.pdf", "text b"));
        resumeMapper.insert(buildResume(2L, "c.pdf", "/tmp/c.pdf", "text c"));

        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getUserId, 1L);
        List<Resume> results = resumeMapper.selectList(wrapper);

        assertEquals(2, results.size());
    }

    @Test
    void logicalDelete() {
        Resume resume = buildResume(1L, "delete.pdf", "/tmp/delete.pdf", "to delete");
        resumeMapper.insert(resume);
        Long id = resume.getId();

        resumeMapper.deleteById(id);

        Resume found = resumeMapper.selectById(id);
        assertNull(found);
    }

    private Resume buildResume(Long userId, String fileName, String filePath, String parsedText) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(fileName);
        resume.setFilePath(filePath);
        resume.setParsedText(parsedText);
        return resume;
    }
}

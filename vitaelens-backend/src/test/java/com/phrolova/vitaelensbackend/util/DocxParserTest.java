package com.phrolova.vitaelensbackend.util;

import com.phrolova.vitaelensbackend.support.ResumeTestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DocxParserTest {

    @Test
    void extractText_validDocx(@TempDir Path tempDir) throws IOException {
        byte[] docxBytes = ResumeTestFixtures.createDocxBytes(ResumeTestFixtures.SAMPLE_DOCX_TEXT);
        File docxFile = tempDir.resolve("sample.docx").toFile();
        Files.write(docxFile.toPath(), docxBytes);

        String text = DocxParser.extractText(docxFile);

        assertNotNull(text);
        assertFalse(text.isEmpty());
        assertTrue(text.contains("张三"));
        assertTrue(text.contains("Spring Boot"));
    }

    @Test
    void extractText_corruptDocx(@TempDir Path tempDir) {
        File corruptFile = tempDir.resolve("corrupt.docx").toFile();

        assertDoesNotThrow(() -> Files.writeString(corruptFile.toPath(), "not-a-docx"));
        assertThrows(Exception.class, () -> DocxParser.extractText(corruptFile));
    }
}

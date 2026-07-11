package com.phrolova.vitaelensbackend.util;

import com.phrolova.vitaelensbackend.support.ResumeTestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfParserTest {

    @Test
    void extractText_validPdf(@TempDir Path tempDir) throws IOException {
        byte[] pdfBytes = ResumeTestFixtures.createPdfBytes(ResumeTestFixtures.SAMPLE_PDF_TEXT);
        File pdfFile = tempDir.resolve("sample.pdf").toFile();
        Files.write(pdfFile.toPath(), pdfBytes);

        String text = PdfParser.extractText(pdfFile);

        assertNotNull(text);
        assertFalse(text.isEmpty());
        assertTrue(text.contains("Zhang San") || text.contains("Java"));
    }

    @Test
    void extractText_corruptPdf(@TempDir Path tempDir) {
        File corruptFile = tempDir.resolve("corrupt.pdf").toFile();

        assertDoesNotThrow(() -> Files.writeString(corruptFile.toPath(), "not-a-pdf"));
        assertThrows(IOException.class, () -> PdfParser.extractText(corruptFile));
    }
}

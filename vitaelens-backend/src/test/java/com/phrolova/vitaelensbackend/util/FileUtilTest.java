package com.phrolova.vitaelensbackend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilTest {

    @Test
    void getExtension_normalFileName() {
        assertEquals("pdf", FileUtil.getExtension("resume.pdf"));
        assertEquals("docx", FileUtil.getExtension("MyResume.DOCX"));
    }

    @Test
    void getExtension_noExtension() {
        assertEquals("", FileUtil.getExtension("readme"));
        assertEquals("", FileUtil.getExtension(".gitignore"));
    }

    @Test
    void getExtension_nullOrEmpty() {
        assertEquals("", FileUtil.getExtension(null));
        assertEquals("", FileUtil.getExtension(""));
    }

    @Test
    void generateFileName_validPdf() {
        String generated = FileUtil.generateFileName("resume.pdf");

        assertTrue(generated.endsWith(".pdf"));
        assertNotEquals("resume.pdf", generated);
        assertEquals(36 + 4, generated.length()); // UUID(36) + ".pdf"(4)
    }

    @Test
    void generateFileName_validDocx() {
        String generated = FileUtil.generateFileName("resume.docx");

        assertTrue(generated.endsWith(".docx"));
    }

    @Test
    void generateFileName_invalidExtension() {
        assertThrows(IllegalArgumentException.class,
                () -> FileUtil.generateFileName("virus.exe"));
    }
}

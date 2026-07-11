package com.phrolova.vitaelensbackend.support;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 简历上传测试用文件与对象构造工具。
 */
public final class ResumeTestFixtures {

    public static final String SAMPLE_PDF_TEXT = "Zhang San\nJava Backend Developer\nSpring Boot experience";
    public static final String SAMPLE_DOCX_TEXT = "张三\nJava 后端开发\nSpring Boot 项目经验";
    public static final Long DEFAULT_USER_ID = 1L;

    private ResumeTestFixtures() {
    }

    public static MockMultipartFile pdfFile() throws IOException {
        return pdfFile("resume.pdf", SAMPLE_PDF_TEXT);
    }

    public static MockMultipartFile pdfFile(String filename, String content) throws IOException {
        byte[] bytes = createPdfBytes(content);
        return new MockMultipartFile("file", filename, "application/pdf", bytes);
    }

    public static MockMultipartFile docxFile() throws IOException {
        return docxFile("resume.docx", SAMPLE_DOCX_TEXT);
    }

    public static MockMultipartFile docxFile(String filename, String content) throws IOException {
        byte[] bytes = createDocxBytes(content);
        return new MockMultipartFile(
                "file",
                filename,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                bytes
        );
    }

    public static MockMultipartFile emptyFile() {
        return new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
    }

    public static MockMultipartFile oversizedPdfFile() {
        return new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[]{0x25}) {
            @Override
            public long getSize() {
                return 11L * 1024 * 1024;
            }
        };
    }

    public static MockMultipartFile invalidContentTypeFile() {
        return new MockMultipartFile("file", "resume.txt", "text/plain", "hello".getBytes());
    }

    public static MockMultipartFile invalidExtensionPdfContentType() {
        return new MockMultipartFile("file", "resume.txt", "application/pdf", "fake".getBytes());
    }

    public static MockMultipartFile corruptPdfFile() {
        return new MockMultipartFile("file", "corrupt.pdf", "application/pdf", "not-a-pdf".getBytes());
    }

    public static byte[] createPdfBytes(String content) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(font, 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(content.replace('\n', ' '));
                stream.endText();
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    public static byte[] createDocxBytes(String content) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(content);
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}

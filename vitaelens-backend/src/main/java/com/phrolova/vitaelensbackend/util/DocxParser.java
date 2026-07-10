package com.phrolova.vitaelensbackend.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
public class DocxParser {

    public static String extractText(File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fileInputStream)){

            StringBuilder stringBuilder = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText().trim();
                if (!text.isEmpty()) {
                    stringBuilder.append(text).append("\n");
                }
            }

            log.info("DOCX 解析完成，提取文本长度： {}",stringBuilder.length());
            return stringBuilder.toString().trim();
        }

    }
}

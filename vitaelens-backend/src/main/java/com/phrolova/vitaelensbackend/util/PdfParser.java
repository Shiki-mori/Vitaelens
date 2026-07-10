package com.phrolova.vitaelensbackend.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

@Slf4j
public class PdfParser {

    public static String extractText(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)){
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("PDF 解析完成，提取文本长度： {}",text.length());
            return text.trim();
        }
     }
}

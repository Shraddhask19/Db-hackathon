package com.querycraft.parser;

import com.querycraft.exception.SchemaParsingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Locale;

@Component
public class PdfSchemaParser implements SchemaParser {

    private static final Logger log = LoggerFactory.getLogger(PdfSchemaParser.class);

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return true;
        }
        return contentType != null && contentType.equalsIgnoreCase("application/pdf");
    }

    @Override
    public String parse(InputStream inputStream, String fileName) throws SchemaParsingException {
        log.info("Parsing PDF schema documentation file: {}", fileName);
        try {
            byte[] bytes = inputStream.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                if (text == null || text.isBlank()) {
                    throw new SchemaParsingException("Extracted text from PDF file is empty: " + fileName);
                }

                log.info("Successfully extracted {} characters from PDF file: {}", text.length(), fileName);
                return "--- PDF Schema Documentation Source: " + fileName + " ---\n" + text.trim();
            }
        } catch (Exception e) {
            log.error("Failed to parse PDF document {}: {}", fileName, e.getMessage());
            throw new SchemaParsingException("Failed to read PDF schema content from file: " + fileName, e);
        }
    }

    @Override
    public String getFileType() {
        return "PDF_DOCUMENT";
    }
}

package com.kortex.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Service for extracting text from documents (PDF, DOCX, DOC, TXT).
 * Handles text cleaning and preprocessing.
 */
@Service
@Slf4j
public class TextExtractionService {

    /**
     * Extract text from a document file.
     *
     * @param file the document file
     * @param fileType the MIME type of the file
     * @return extracted and cleaned text, or null if extraction failed
     */
    public String extractText(File file, String fileType) {
        try {
            String rawText = switch (fileType) {
                case "application/pdf" -> extractFromPdf(file);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> extractFromDocx(file);
                case "application/msword" -> extractFromDoc(file);
                case "text/plain" -> extractFromTxt(file);
                default -> {
                    log.warn("Unsupported file type for text extraction: {}", fileType);
                    yield null;
                }
            };

            if (rawText == null || rawText.isBlank()) {
                log.warn("No text extracted from file: {}", file.getName());
                return null;
            }

            // Clean and preprocess text
            return cleanText(rawText);

        } catch (Exception e) {
            log.error("Error extracting text from file: {}", file.getName(), e);
            return null;
        }
    }

    /**
     * Extract text from PDF file using Apache PDFBox.
     */
    private String extractFromPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Extract text from DOCX file using Apache POI.
     */
    private String extractFromDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * Extract text from DOC file using Apache POI.
     */
    private String extractFromDoc(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * Extract text from TXT file.
     */
    private String extractFromTxt(File file) throws IOException {
        return Files.readString(file.toPath());
    }

    /**
     * Clean and preprocess extracted text.
     * - Remove excessive whitespace
     * - Remove control characters
     * - Normalize line breaks
     * - Trim
     *
     * @param rawText the raw extracted text
     * @return cleaned text
     */
    private String cleanText(String rawText) {
        if (rawText == null) {
            return null;
        }

        // Remove control characters (except newlines, tabs, carriage returns)
        String cleaned = rawText.replaceAll("[\\p{Cntrl}&&[^\n\r\t]]", "");

        // Normalize line breaks (convert \r\n and \r to \n)
        cleaned = cleaned.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");

        // Remove excessive blank lines (more than 2 consecutive newlines)
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");

        // Normalize whitespace (replace multiple spaces/tabs with single space)
        cleaned = cleaned.replaceAll("[ \t]+", " ");

        // Trim each line
        String[] lines = cleaned.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            result.append(line.trim()).append("\n");
        }

        return result.toString().trim();
    }

    /**
     * Get estimated page count from extracted text (rough approximation).
     * Assumes ~500 words per page.
     *
     * @param text the extracted text
     * @return estimated page count
     */
    public Integer estimatePageCount(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        // Count words (split by whitespace)
        String[] words = text.split("\\s+");
        int wordCount = words.length;

        // Estimate pages (500 words per page)
        int pages = (int) Math.ceil(wordCount / 500.0);
        return Math.max(1, pages); // At least 1 page
    }
}

package com.sleepwell.sleepwell_backend.rag.infra.extract;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.sleepwell.sleepwell_backend.rag.infra.extract.DocExtractor.ExtractedPart;

// PDFBox: 페이지 단위 추출

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/** 문서 추출: Tika + (PDF일 때) 페이지 단위 + OCR(옵션) */
@Component
public class TikaDocExtractor implements DocExtractor {

    private final boolean ocrEnabled;
    private final String tesseractPath;
    private final String ocrLang;
    private final Tika tika = new Tika();

    public TikaDocExtractor(
            @Value("${rag.tika.ocr.enabled:true}") boolean ocrEnabled,
            @Value("${rag.tika.ocr.tesseractPath:}") String tesseractPath,
            @Value("${rag.tika.ocr.lang:kor+eng}") String ocrLang) {
        this.ocrEnabled = ocrEnabled;
        this.tesseractPath = tesseractPath;
        this.ocrLang = ocrLang;
    }

    @Override
    public List<ExtractedPart> extract(Path file) throws IOException {
        String ext = FilenameUtils.getExtension(file.getFileName().toString()).toLowerCase(Locale.ROOT);
        if ("pdf".equals(ext)) {
            return extractPdfPerPage(file);
        }
        // 그 외 포맷: Tika 한 방 추출
        return List.of(ExtractedPart.of(tikaExtractAll(file), null, detectMime(file)));
    }

    /* ---------- PDF: 페이지 단위 ---------- */

    private List<ExtractedPart> extractPdfPerPage(Path pdf) throws IOException {
        List<ExtractedPart> parts = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int n = doc.getNumberOfPages();
            for (int i = 1; i <= n; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = StringUtils.trimToEmpty(stripper.getText(doc));
                // 페이지 텍스트가 너무 빈약하면 OCR 폴백 (선택)
                if (ocrEnabled && text.codePoints().count() < 40) {
                    String ocrText = ocrPageWithTika(pdf, i);
                    if (StringUtils.isNotBlank(ocrText)) text = ocrText;
                }
                if (StringUtils.isNotBlank(text)) {
                    parts.add(ExtractedPart.of(clean(text), i, "application/pdf"));
                }
            }
        } catch (Exception e) {
            // PDF 파싱 실패 시 전체 OCR 시도 대신, Tika 전체 추출로 폴백
            parts.add(ExtractedPart.of(tikaExtractAll(pdf), null, "application/pdf"));
        }
        return parts;
    }

    /* ---------- Tika 추출 (비PDF or OCR 폴백) ---------- */

    private String tikaExtractAll(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            var handler = new BodyContentHandler(-1);
            var metadata = new Metadata();
            var parser = new AutoDetectParser(TikaConfig.getDefaultConfig());
            var ctx = new ParseContext();
            if (ocrEnabled) {
                TesseractOCRConfig cfg = new TesseractOCRConfig();
                if (StringUtils.isNotBlank(tesseractPath)) {
// 폴더(윈도우: C:\\Program Files\\Tesseract-OCR)
                }
                cfg.setLanguage(ocrLang); // "kor+eng"
                ctx.set(TesseractOCRConfig.class, cfg);
            }
            parser.parse(in, handler, metadata, ctx);
            return clean(handler.toString());
        } catch (Exception e) {
            // 마지막 정말 안되면 간단 모드
            try {
                return clean(tika.parseToString(path));
            } catch (org.apache.tika.exception.TikaException e2) {
                return "";
            } catch (Exception e3) {
                return "";
            }
        }
    }

    private String ocrPageWithTika(Path pdf, int page) {
        // 간단/안전: per-page OCR 대신 전체 OCR 텍스트를 폴백으로 사용
        try {
            return tikaExtractAll(pdf);
        } catch (IOException ignored) {
            return "";
        }
    }

    private static String detectMime(Path p) {
        try { return Files.probeContentType(p); }
        catch (Exception ignore) { return "application/octet-stream"; }
    }

    private static String clean(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }
}





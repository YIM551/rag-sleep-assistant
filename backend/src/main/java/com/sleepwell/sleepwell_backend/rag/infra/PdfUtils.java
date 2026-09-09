package com.sleepwell.sleepwell_backend.rag.infra;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;

import java.io.File;
import java.util.*;

public class PdfUtils {
    public static List<Document> read(String path) {
        List<Document> out = new ArrayList<>();
        File f = new File(path);
        String filename = f.getName();
        String source = f.toURI().toString();

        try (PDDocument pdf = PDDocument.load(f)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pages = pdf.getNumberOfPages();
            for (int i = 1; i <= pages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(pdf);
                if (text != null && !text.isBlank()) {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("filename", filename);
                    meta.put("source", source);
                    meta.put("page", i);
                    out.add(new Document(text, meta));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("PDF read failed: " + path, e);
        }
        return out;
    }
}





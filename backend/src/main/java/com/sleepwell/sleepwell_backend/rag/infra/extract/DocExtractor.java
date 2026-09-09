package com.sleepwell.sleepwell_backend.rag.infra.extract;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** 문서(확장자 무관)에서 인덱싱할 텍스트 조각을 추출하는 공통 인터페이스 */
public interface DocExtractor {
    /**
     * 주어진 파일 경로에서 텍스트 조각을 추출한다.
     * - PDF의 경우 페이지 단위로 쪼갠 조각을 반환 (page >= 1)
     * - 기타 문서는 1개 조각(page == null)일 수 있음
     */
    List<ExtractedPart> extract(Path file) throws IOException;

    /** 추출 결과 단위 */
    record ExtractedPart(String text, Integer page, String mimeType) {
        public static ExtractedPart of(String text, Integer page, String mimeType) {
            return new ExtractedPart(text, page, mimeType);
        }
    }
}

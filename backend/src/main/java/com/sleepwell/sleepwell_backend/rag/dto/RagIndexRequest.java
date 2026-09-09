package com.sleepwell.sleepwell_backend.rag.dto;

import java.util.List;
import java.util.Map;

public record RagIndexRequest(
    String namespace,          // ?? "sleepwell-ko@v1"
    boolean overwrite,         // 湲곗〈 ?몃뜳??援먯껜 ?щ?
    List<Doc> docs
) {
  public record Doc(
      String id,
      String title,
      String url,
      String text,
      Map<String,String> meta   // lang/topic/stage/trust ??
  ) {}
}


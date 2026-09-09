package com.sleepwell.sleepwell_backend.rag.infra.impl;

import com.sleepwell.sleepwell_backend.rag.infra.EmbeddingClient;
import java.util.ArrayList;
import java.util.List;

/** ?붾? ?꾨쿋?? ?좏겙 鍮덈룄 湲곕컲. ?ㅼ꽌鍮꾩뒪?먯꽌???ㅼ젣 ?꾨쿋?⑹쑝濡?援먯껜 */
public class SimpleEmbeddingClient implements EmbeddingClient {
  @Override
  public List<float[]> embed(List<String> texts) {
    List<float[]> out = new ArrayList<>();
    for (String t : texts) {
      float[] v = new float[256];
      for (String tok : t.toLowerCase().split("\\W+")) {
        v[Math.abs(tok.hashCode()) % 256] += 1f;
      }
      out.add(v);
    }
    return out;
  }
}


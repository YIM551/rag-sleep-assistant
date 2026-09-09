package com.sleepwell.sleepwell_backend.rag.infra;

import java.util.List;
import java.util.Map;

public interface PromptBuilder {
  record Cite(String title, String url, String quote){}
  record Built(String system, String user, List<Cite> cites){}
  Built build(String query, List<Cite> contexts, Map<String,Object> profile);
}


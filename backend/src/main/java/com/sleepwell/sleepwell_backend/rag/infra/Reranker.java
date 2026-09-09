package com.sleepwell.sleepwell_backend.rag.infra;

import java.util.List;
import java.util.function.ToDoubleFunction;

public interface Reranker {
  <T> List<T> mmr(List<T> items, int topK, ToDoubleFunction<T> scoreFn, double lambda);
}


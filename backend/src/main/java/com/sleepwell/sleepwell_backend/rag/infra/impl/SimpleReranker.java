package com.sleepwell.sleepwell_backend.rag.infra.impl;

import com.sleepwell.sleepwell_backend.rag.infra.Reranker;
import java.util.*;
import java.util.function.ToDoubleFunction;

public class SimpleReranker implements Reranker {
  @Override public <T> List<T> mmr(List<T> items, int topK, ToDoubleFunction<T> scoreFn, double lambda){
    items.sort((a,b)->Double.compare(scoreFn.applyAsDouble(b),scoreFn.applyAsDouble(a)));
    return items.subList(0, Math.min(topK, items.size()));
  }
}


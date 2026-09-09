package com.sleepwell.sleepwell_backend.rag.infra.adapter;

import com.sleepwell.sleepwell_backend.rag.infra.port.SparseRetrieverPort;
import com.sleepwell.sleepwell_backend.rag.model.ScoredDoc;
import com.sleepwell.sleepwell_backend.rag.infra.SparseStore;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

@Component
public class SparseAdapter implements SparseRetrieverPort {
    private final SparseStore sparseStore;
    public SparseAdapter(SparseStore store){ this.sparseStore = store; }

    @Override
    public List<ScoredDoc> search(String query, int topK) {
        List<?> raw = invokeFirstAvailable(sparseStore, query, topK,
                new String[]{"search","bm25","keywordSearch"});
        return toScoredDocs(raw, "sparse");
    }

    @SuppressWarnings("unchecked")
    private static List<?> invokeFirstAvailable(Object target, String q, int k, String[] names){
        for (String n: names){
            try {
                Method m = target.getClass().getMethod(n, String.class, int.class);
                return (List<?>) m.invoke(target, q, k);
            } catch (NoSuchMethodException ignored){}
              catch (Exception e){ throw new RuntimeException(e); }
            try {
                Method m2 = target.getClass().getMethod(n, String.class);
                Object r = m2.invoke(target, q);
                if (r instanceof List<?>) return (List<?>) r;
            } catch (NoSuchMethodException ignored){}
              catch (Exception e){ throw new RuntimeException(e); }
        }
        return Collections.emptyList();
    }

    private static List<ScoredDoc> toScoredDocs(List<?> in, String source){
        List<ScoredDoc> out = new ArrayList<>();
        if (in==null) return out;
        for (Object o: in){
            if (o instanceof ScoredDoc sd){ out.add(sd); continue; }
            try {
                Map<String,Object> meta = tryMeta(o);
                String id = firstNonNull(
                        metaString(meta,"id"),
                        metaString(meta,"docId"),
                        tryString(o,"getId","id"),
                        UUID.randomUUID().toString()
                );
                String content = firstNonNull(
                        metaString(meta,"text"),
                        metaString(meta,"content"),
                        tryString(o,"getContent","content","getText","text"),
                        o.toString()
                );
                Double score = firstNonNullDouble(
                        tryDouble(o,"getScore","score"),
                        numberToDouble(meta.get("score")),
                        0.0
                );
                String src = firstNonNull(
                        metaString(meta,"source"),
                        metaString(meta,"url"),
                        source
                );
                out.add(new ScoredDoc(id, content, score, src, meta));
            } catch (Exception ignored) {
                out.add(new ScoredDoc(UUID.randomUUID().toString(), o.toString(), 0.0, source, Map.of()));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> tryMeta(Object o) throws Exception {
        try {
            Method m = o.getClass().getMethod("getMetadata");
            Object v = m.invoke(o);
            if (v instanceof Map<?,?> m1){
                Map<String,Object> copy = new HashMap<>();
                m1.forEach((k,val)-> copy.put(String.valueOf(k), val));
                return copy;
            }
        } catch (NoSuchMethodException ignored) {}
        return new HashMap<>();
    }

    private static String tryString(Object o, String... names) throws Exception {
        for (String n: names){
            try {
                Method m = o.getClass().getMethod(n);
                Object v = m.invoke(o);
                if (v!=null) return String.valueOf(v);
            } catch (NoSuchMethodException ignored){}
        }
        return null;
    }
    private static Double tryDouble(Object o, String... names) throws Exception {
        for (String n: names){
            try {
                Method m = o.getClass().getMethod(n);
                Object v = m.invoke(o);
                if (v instanceof Number num) return num.doubleValue();
                if (v!=null) return Double.parseDouble(v.toString());
            } catch (NoSuchMethodException ignored){}
        }
        return null;
    }

    private static String metaString(Map<String,Object> m, String k){
        Object v = (m==null)?null:m.get(k);
        return v==null? null : String.valueOf(v);
    }
    private static <T> T firstNonNull(T... vals){
        for (T v: vals) if (v!=null) return v;
        return null;
    }
    private static Double firstNonNullDouble(Double... vals){
        for (Double v: vals) if (v!=null) return v;
        return null;
    }
    private static Double numberToDouble(Object o){
        return (o instanceof Number n)? n.doubleValue() : null;
    }
}

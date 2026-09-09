# 지연 측정과 병렬 실험

## 현재 서비스에 추가한 단계 측정

2026년 `RagQueryService.answer()`에 `System.nanoTime()` 기반 성공 경로 측정을 추가했습니다. `RAG_STAGE_TIMING {stage=value,...}` 로그에는 질문·profile·근거 원문을 넣지 않습니다.

| 필드 | 포함 범위 |
|---|---|
| query_expansion_ms | 규칙 확장 + LLM rewrite + PRF seed 검색 |
| retrieval_ms | 최대3개 확장에 대한 현재 순차 hybrid 검색, 내부 RRF/MMR, keyword/namespace filtering, dedup |
| reranking_ms | 통합 후보 정렬·추가 MMR·규칙 reranker |
| prompt_build_ms | 선택 citations와 query/profile 조합 |
| llm_ms | 최종 답변 생성 호출 |
| total_ms | 서비스 메서드 성공 경로 전체; validation·문맥 변환 등의 잔여 비용 포함 |

`retrieval_ms`는 최종 생성뿐 아니라 query-expansion의 rewrite/PRF도 제외합니다. 순수 DB 시간만 나타내는 값은 아니며 내부 RRF/MMR이 포함됩니다. 응답 실패와 red-flag 조기 반환은 이 timing 이벤트를 기록하지 않아 실패율은 계산할 수 없습니다. 외부 전송·controller/network 시간도 별도입니다.

## 측정 및 분석 절차

현재 환경에서는 실제 OpenAI/Qdrant 호출을 하지 않았으므로 신규 서비스 성능 숫자는 없습니다. 같은 corpus, model, config, warmup 조건의 한 실행에서 성공 timing 로그를 수집하고 아래 분석기를 사용합니다.

```bash
python scripts/benchmark_retrieval.py
python scripts/benchmark_retrieval.py --log path/to/single-run.log
```

기본은 dry-run이며 `--log`도 네트워크를 호출하지 않습니다. 평균과 nearest-rank p95, 성공 레코드 수를 계산합니다. 실패·timeout 비율, concurrency, throughput, 측정 cohort 동질성은 이 로그만으로 검증할 수 없습니다. 여러 실행을 섞거나 warmup을 포함하지 않도록 먼저 분리해야 합니다. JUnit의 합성 timing 로그를 실제 서비스 성능으로 사용하지 않습니다.

## 과거 Serial vs Parallel

현재 `HybridRetriever`는 순차입니다. 별도 `origin/feat/rag-parallel-retrieval-v3`의 `RagQueryService`는 sequential/old/async 전략과 executor를 포함합니다. [실제 코드·원자료를 보존한 실험 저장소](https://github.com/YIM551/rag-retrieval-benchmark)를 확인하세요. 과거 보고서의 p95를 평균이라고 부르거나 요청30건을 동시사용자30명으로 바꾸지 않습니다.

## 로컬 부하 스크립트

[k6 시나리오](../tests/load/README.md)는 원격 URL을 거부하고 기본 dry-run, 명시적 stub opt-in, VUS/iteration 상한을 둡니다. 이번에는 k6 엔진 실행 및 HTTP 부하를 수행하지 않았습니다. JavaScript syntax 검사만 완료했고 실제 처리량·동시사용자 수는 미측정입니다.

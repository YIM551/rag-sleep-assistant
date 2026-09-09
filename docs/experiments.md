# 실험 근거와 해석

## 2026 코드 검증

- 팀 백엔드 현재 소스를 Java 17 / 원 Gradle dependency 설정으로 컴파일하고 bootJar를 만들었습니다.
- 새 Java 계약 테스트 6개는 VectorStore 대역, 입력 validation, 현재 chunk 종료/재구성, 합성 질의→인용→프롬프트 흐름을 검사합니다. API·Qdrant·LLM을 호출하지 않습니다.
- Python timing 분석기 테스트 3개는 nearest-rank p95, 비정상/누락/비유한 duration, 빈 데이터 오인 방지를 검사합니다.
- 실제 서비스 전체 기동·Docker 이미지 실행·유료 API·의료 품질·운영 부하는 측정하지 않았습니다.

## 캡스톤 보고서 품질 평가

인쇄 pp.35–37의 RAGAS 결과는 Retrieval-only recall 0.833 / precision 0.796, E2E faithfulness 0.823 등입니다. 고정 문맥과 E2E는 동일 입력·프롬프트 조건의 모델 개선 전후라고 확인되지 않아 수치 차이를 인과적 개선율로 계산하지 않습니다.

28명 A/B 평가라고 서술했으나 본문 선택 인원은 baseline12 + RAG15 = 27명입니다. 나머지1명의 처리·원설문·모델 출력이 확인되어야 합니다. 요약판의54%,4.2/5도 원자료가 없는 보고서 값입니다. 본문과 요약의 만족도 항목 해석도 일치하지 않아 세부 결론을 확대하지 않습니다.

표4-5에는 10명 SSE/20건 RAG 호출의 PASS 및 지연 수치가 있지만 실행 로그·부하 설정을 같은 실행으로 연결하지 못했습니다. 이는 동시 사용자 검증 완료나 운영 capacity 근거로 사용할 수 없습니다. 보고서 자체도 상용 수준 대규모 부하 검증의 한계를 명시합니다.

## 개인 검색 병렬화 후속 실험

[rag-retrieval-benchmark](https://github.com/YIM551/rag-retrieval-benchmark)는 다른 branch의 실행 전략·서버 원로그·요청별 timing 및 CSV를 다룹니다. 현재 소스의 순차 Dense/Sparse 호출 및 현재 발견한 Dense 연결 버그를 과거 실행과 섞지 않습니다. 측정 단위, mean/p95, request count, mode/thread/filter/collection 조건은 별도 실험 문서를 기준으로 설명합니다.

## 다음 실험

재배포 가능한 corpus/질문/정답근거와 revision을 고정하고, 실제 두 저장소에 일치하는 document ID를 적재한 뒤 retrieval quality와 retrieval-only 지연을 함께 측정합니다. 운영 부하는 별도의 외부 API 비용과 개인정보 정책을 준비한 뒤 수행할 과제입니다.

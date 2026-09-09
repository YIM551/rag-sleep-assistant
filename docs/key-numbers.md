# 면접 핵심 숫자

| 항목 | 값 | 근거 / 의미 |
|---|---|---|
| Runtime Java / Boot / Spring AI | 17 / 3.5.0 / 1.0.3 | backend/build.gradle |
| 현재 embedding | text-embedding-3-large | backend/src/main/resources/application-rag.yml |
| Dense / Sparse top-k | 30 / 60 | 같은 config, runtime override 가능 |
| RRF k | 60 | 같은 config |
| MMR k / lambda | 25 / 0.45 | 같은 config |
| rerank 후보 | 20 | 같은 config |
| 최종 query top-k 기본값 | 5 | RagQueryService, 인용 중복제거 후 더 적어질 수 있음 |
| 최대 확장 검색 질의 | 3 | RagQueryService |
| 문자 chunk / overlap | 1200 / 200 | RagIndexService, 토큰 수가 아님 |
| VectorStore add batch | 64 | RagIndexService |
| 보관 PDF 수 | 16 | corpus-manifest.json; 실행별 corpus manifest 아님 |
| 보고서 recall / precision | 0.833 / 0.796 | retrieval-only; 현재 재측정 아님 |
| 보고서 E2E faithfulness | 0.823 | 원평가 재현 미완료 |
| A/B 응답자 | 보고서28, 선택12+15=27 | 누락1명 처리 확인 필요 |
| Stage3 Mistral ID | mistralai/Mistral-7B-Instruct-v0.3 | 별도 학습 config; 실제 checkpoint revision 확인 필요 |
| Java / Python 신규 테스트 | 6 / 3 | 2026 계약·합성 테스트 |
| 실제 서비스 동시사용자 | 확인 필요 | 이번 작업 부하 실행 없음 |
| 새 서비스 평균 / p95 | 확인 필요 | timing 코드만 추가, 유료 환경 미실행 |

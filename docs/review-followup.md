# 2026-09-10 수정과 기존 검토의 정정

2026-09-09 검토는 학교 제출 소스에 한정되어 현재 로컬 서비스와 개인 실험 branch를 놓쳤습니다. 이번에는 현재 `004ad3a`를 기준으로 공개 소스를 복구했습니다. 사용자가 팀 코드 공개를 허용했으며 원본 Git history는 가져오지 않았습니다.

| 변경 | 이유 / 범위 |
|---|---|
| backend 소스·Gradle·migration·prompt·RAG config 공개 | 현재 팀 서비스의 실제 코드 근거. 전체 단독 기여 주장 아님 |
| DenseAdapter typed VectorStore | 검색 메서드가 없는 embedding client에 reflection 호출하던 연결 오류 수정 |
| RagQueryService 입력 검증 | null/blank/4000자초과 query, top-k1..50 밖 입력을 외부 호출 전에 거부 |
| RAG query·expansion 로그 최소화 | 원문·키워드 대신 길이/개수·오류 종류 |
| private Cacheable 제거 | self-call 프록시 미적용 상태에서 캐시·초단위 절약을 주장하던 annotation/주석 정리 |
| 단계별 성공 timing 로그 | expansion/retrieval/rerank/prompt/LLM/total 구분. 실패율·운영성능 결과 없음 |
| Docker wrapper / fail-fast | 원 wrapper 사용, dependency 실패 무시 제거 |
| JWT/더미 데이터 | 고정 JWT 대신 환경변수, 합성 사용자 생성은 local-fixture opt-in |
| Streamlit 초기 소스 | 별도 legacy로 보존, cbti/TTS import 경로 및 사용하지 않는 누락 import 정리 |
| tests / analyzer / local k6 | 비용 없는 계약 테스트와 향후 로컬 측정 절차 |
| demo / 문서 | 실제 시연 포함, 개인 기여·모델·평가·재현·한계를 원자료에 연결 |

현재 RagIndexService에는 마지막 chunk 종료조건, overlap clamp, Lucene/Qdrant 이중 색인이 이미 존재했습니다. 제출본용 private.patch를 그대로 적용하지 않았습니다. 현재 6개 Java 계약 검사를 통과했으며 이를 2025 제출 당시 테스트 결과로 소급하지 않습니다.

출처별 원본 SHA256와 공개 파일 SHA256, 수정 이유는 [source-manifest.json](source-manifest.json)에 있습니다. 개인정보 치환·2026 최소 수정과 원본 알고리즘을 구분합니다. API·DB·클라우드·의료 품질·전체 팀 서비스의 보안 검증은 끝난 것으로 표시하지 않습니다.

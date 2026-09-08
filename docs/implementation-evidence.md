# 구현 근거와 공개 보류 범위

원본 기준 경로의 공통 접두사는 `src/main/java/com/sleepwell/sleepwell_backend/`입니다. 아래 파일은 팀 소스 공개 동의 확인 전까지 공개 저장소에 복제하지 않았습니다.

| 구현 파일 | 읽어 확인한 동작 | 해석의 한계 |
| --- | --- | --- |
| rag/service/RagQueryService.java | red-flag 검사, 확장 질의 3개 제한, namespace/키워드 필터, 중복 제거, 문맥 구성, chat 호출 | private 메서드의 Cacheable은 프록시 경유 여부 검증 필요 |
| rag/infra/impl/HybridRetriever.java | 순차 Dense/Sparse 호출, 정규화, RRF, 선택적 MMR | 병렬 실행이라고 표시하지 않음 |
| rag/infra/adapter/DenseAdapter.java | 임베딩 클라이언트의 검색 메서드를 reflection으로 찾음 | 해당 클라이언트에 검색 메서드가 없어 제출본은 빈 결과 반환. private 사본에서 VectorStore 직접 호출로 수정 |
| rag/infra/SparseStore.java | Lucene FSDirectory, Nori, BM25, 파일/namespace 메타데이터 | 주입 기본 청크 1200/overlap 200은 실제 활성 환경과 다를 수 있음 |
| rag/service/RagIndexService.java | legacy vector index와 파일 sparse index | 종료 조건 및 양쪽 저장소 갱신 불일치 발견 |
| rag/infra/adapter/RerankerAdapter.java | 기본 점수·키워드 겹침·도메인 키워드 보너스의 가중합 | 코드의 RBF는 Relevance Boost Fusion이라는 자체 명칭이며 학습한 Radial Basis Function 모델 증거가 아님 |
| rag/infra/SpringAiChatClient.java, rag/infra/impl/ClinicalRagPromptBuilder.java | 질문·프로필·인용 문맥으로 OpenAI 모델 호출 | 기기 내 처리 또는 외부 전송 없음으로 설명할 수 없음 |
| service/impl/ConsultationServiceImpl.java | 세션 사용자 확인 후 사용자·AI 메시지를 서버 repository에 저장 | 모든 개인정보가 기기에만 남는다는 설명과 다름 |
| build.gradle | Java 17, Spring Boot 3.5.0, Spring AI 1.0.3, Lucene 9.9.1 | dependency 선언 자체가 실제 기능 사용 증명은 아님 |

## 실행 상태

공개본은 문서 저장소입니다. 원본 빌드를 성공했다고 주장하지 않습니다. 원본에 실행 오류 가능성이 있고, 전체 팀 소스를 공개할 권한과 외부 서비스 환경을 확인해야 합니다.

2026-09-09 private 사본에서 레거시 청크 종료 조건 및 실제 청크 전달을 수정하고, Dense 어댑터를 Spring AI VectorStore API에 연결했습니다. 원본을 보존한 상태에서 청크 메서드 회귀 검사와 실제 Spring AI 1.0.3 API를 이용한 로컬 계약 검사를 수행했습니다. 이는 전체 앱 빌드·실제 Qdrant 검색·배포 검증을 대신하지 않습니다. [검증 세부사항](review-followup.md)에 실행 범위를 기록했습니다.

## 문서 상충 처리

초기 발표의 200–400자 청크, top-k 4–6, GPT-3.5는 초기 설계 설명입니다. 최종 제출 소스의 기본값으로 옮겨 적지 않았습니다. 보고서의 병렬화 후속 실험과 제출본의 순차 검색도 별도로 표시합니다.

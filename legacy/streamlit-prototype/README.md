# 초기 수면 상담 Python 프로토타입

현재 Java 서비스 이전의 소스입니다. 기존 흐름을 보존해 Pinecone/MiniLM 검색, persona, 감정 분류, 음성 처리의 연결을 읽을 수 있게 했습니다. Java 버전 또는 영상에서 실행한 정확한 commit과 동일하다고 보장하지 않습니다.

- ChatOpenAI 기본 config: `gpt-4`; embedding: `sentence-transformers/all-MiniLM-L6-v2`; Pinecone top-k3.
- 감정 분류: `j-hartmann/emotion-english-distilroberta-base` CPU. 한국어 정확도 검증 없음.
- RAG init 실패 시 일반 chat fallback. 전용 ingestion/data ID와 실제 index metadata는 미복구.
- cbti_week_prompts 실제 위치와 TTS 함수 import를 수정했습니다. 누락 상수의 사용하지 않는 import도 제거했습니다.
- 원 `requirements.txt`, persona images, 환경 버전, Pinecone corpus가 없어 완전 실행 패키지로 간주하지 않습니다. `requirements.in`은 import에서 확인한 dependency 이름이며 당시 pin을 복구한 lockfile이 아닙니다.
- 일부 대체 UI helper의 `get_response` 호출 인자와 현재 service signature, 음성/CBTI 주차 연결은 아직 통합 검증되지 않았습니다. 원 흐름을 대규모로 재작성하지 않았습니다.
- `.streamlit/secrets.toml`, 로컬 wheel·cache·개인 경로·실제 API key를 제외했습니다. 원 secret 값을 예제에 복사하지 않았습니다.

검증한 것은 Python source syntax와 명시적 internal module 경로입니다. `streamlit run run.py`에 앞서 dependency/이미지/Pinecone/API 환경과 남은 helper mismatch를 복구해야 하며, 실행 시 외부 API 비용이 생길 수 있습니다.

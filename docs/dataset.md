# 데이터 출처와 처리

## RAG 문헌

로컬 `rag-corpus`에서 PDF 16개를 확인했습니다. 수면·갱년기·불면·CBT-I·이완 관련 논문과 서적이 섞여 있습니다. 이 폴더 수는 특정 실험 실행에서 실제 적재한 원문·청크 수를 입증하지 않습니다. `RagController`의 과거 7,425개 청크 문구는 검증 가능한 실행 manifest 없이 하드코딩되어 있어 제거했습니다.

[corpus-manifest.json](corpus-manifest.json)에는 파일명·bytes·SHA256·페이지 수·원문에서 추출한 DOI 후보를 기록했습니다. DOI 후보는 파일 본문에서 추출했으며 제목/DOI 매칭과 재배포 라이선스를 확정한 목록은 아닙니다. 원문 PDF, 추출 전문, 벡터, Lucene index는 포함하지 않습니다. 상업 서적 및 개별 재배포 허락이 확인되지 않은 문헌의 공개를 피했습니다.

| 항목 | 확인 내용 |
|---|---|
| Source / 식별자 | 각 로컬 PDF와 corpus manifest의 DOI 후보 |
| Format / 수 | PDF 16개, 원실험 유효 청크 수 미확인 |
| License | 개별 재배포 권한 미확인 → 원문 미공개 |
| Usage | 질의에 관련된 문헌 근거 검색 |
| Preprocessing | PDFBox/Tika 추출 → 문자 청크 → metadata → Lucene/Qdrant |
| Storage | Lucene BM25/Nori + Spring AI Qdrant VectorStore |
| 개인정보 | 사용자 실제 질문·건강 기록·profile은 데이터셋에 포함하지 않음 |

현재 파일 색인은 namespace/filename/source/filePath/chunkIndex/chunkId/docId/ingestId를 만듭니다. 경로 기반 source는 citations 단계에서 숨기지만 내부 metadata에는 저장될 수 있습니다. public 샘플은 실제 개인정보가 없는 합성 텍스트만 사용합니다.

## 초기 Pinecone 프로토타입

`legacy/streamlit-prototype/src/utils/rag_utils.py`는 이미 존재하는 Pinecone index에서 content/disease/tab metadata를 읽습니다. 원 ingestion script, dataset의 공식 ID, 정확한 row 수, license는 복구하지 못했습니다. 이를 Java PDF corpus와 동일 데이터셋이라고 주장하지 않습니다.

## Fine-tuning 데이터

모델 학습 실험은 [sleep-llm-finetuning의 dataset 문서](https://github.com/YIM551/sleep-llm-finetuning/blob/master/docs/dataset.md)에 연결합니다. 설정에 적힌 dataset ID·로드 시도와 실제 학습에 사용된 샘플 수를 구분합니다. RAG 문헌 수를 fine-tuning 샘플 수로 대체하지 않습니다.

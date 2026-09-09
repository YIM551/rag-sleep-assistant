# 수면 도메인 Fine-tuning 실험과 서비스의 관계

현재 RAG 서비스의 `SpringAiChatClient`는 OpenAI 모델을 호출합니다. 학습한 adapter를 이 경로에 배포했다는 증거는 확인하지 못했습니다. 전체 학습·평가 소스는 [sleep-llm-finetuning](https://github.com/YIM551/sleep-llm-finetuning)에 별도로 보존합니다.

| 질문 | 확인한 사실 | 한계 |
|---|---|---|
| 정확한 Mistral 버전 | Stage3 config의 `mistralai/Mistral-7B-Instruct-v0.3` | 실제 checkpoint revision을 확보한 것은 아님 |
| 모든 Stage가 같은 모델인가 | Stage1/2 기본 YAML은 Meta-Llama-3-8B-Instruct, 평가 라벨은 base_mistral7b | 모델 라벨 불일치로 Stage 전체를 Mistral이라고 확정할 수 없음 |
| LoRA / QLoRA | NF4 4-bit + LoRA rank64/alpha16/dropout0.05, attention/MLP projection targets 설정 | 실제 실행별 완료 로그·checkpoint와 대조 필요 |
| 학습 조건 | batch2, accumulation16, LR2e-4, factual2/counsel1 epoch 설정 | config 값이며 모든 run에서 실행된 값이라는 뜻은 아님 |
| Stage3 scratch/warmstart | 두 YAML에 동일 Mistral ID | 원 trainer와 YAML 키·enabled·init_adapter 연결 문제가 확인됨 |
| 성능 | 원보고서 74/90/92% judge 결과 존재 | Reference를 GPT-4o-mini에 제공한 후처리 답변 평가여서 순수 fine-tuning 성능으로 주장하지 않음 |

목표는 수면 지식과 상담 응답 방식에 모델을 맞추는 것이었습니다. 실패한 초기 결과와 후속 실험을 숨기지 않고, 데이터 규모·언어·기본 모델·평가 설정을 확인하는 과정으로 설명합니다. 낮은 성능의 원인을 한 요소로 확정하지 않습니다.

Instruct 선택은 상담형 질의응답이라는 목적과 잘 맞지만, Base/Instruct 비교 실험이 확인되지 않았으므로 “비교 결과 더 좋아서 선택했다”고 말하지 않습니다. 출발 모델, 학습 실행, 평가 후처리, 실제 서비스 모델을 분리하는 것이 핵심입니다.

[상세 학습 설정](https://github.com/YIM551/sleep-llm-finetuning/blob/master/docs/fine-tuning.md) · [데이터](https://github.com/YIM551/sleep-llm-finetuning/blob/master/docs/dataset.md) · [실험 해석](https://github.com/YIM551/sleep-llm-finetuning/blob/master/docs/experiments.md)

# 실행과 검증 범위

## 재현한 범위

JDK17과 프로젝트에 있던 Gradle9.3.0 wrapper를 사용합니다. Spring Boot3.5.0 / Spring AI1.0.3 / Lucene9.9.1 의존성 선언을 보존했습니다. 초기 offline compile은 성공했으며 runtime/test 의존성 일부가 캐시에 없어 저장소에서 받은 후 아래 명령을 통과했습니다. dependency 버전을 임의로 최신화하지 않았습니다.

```powershell
cd backend
.\gradlew.bat test bootJar --no-daemon
```

macOS/Linux에서는 `chmod +x gradlew` 후 `./gradlew test bootJar --no-daemon`을 사용합니다. 새 Java 테스트6개가 실행되며 전체 팀 서비스의 과거 테스트 스위트가 아닙니다. 테스트가 Spring Boot 서버를 시작하지 않고 Mockito 대역만 사용하므로 OpenAI/Qdrant/AWS/DB 요청이 없습니다. 빌드 결과 JAR는 로컬 build 산출물이고 Git에는 올리지 않습니다.

```bash
# repository root, Python3 standard library only
python -m unittest discover -s tests -v
python scripts/benchmark_retrieval.py
```

## 통합 실행에 필요한 별도 준비

전체 팀 backend에는 인증/OAuth, JPA, 알림/Firebase, AWS, 여러 LLM, 결제 등 추가 의존성이 있습니다. 현재 포함한 `application.yml`과 `application-rag.yml`은 원본 현재 설정을 기반으로 한 공개용 예시이며 비밀키는 없습니다. 공개되지 않은 dev/prod/docker/test 환경파일은 재구성해야 합니다. `.env.example`의 키 이름을 참고하되 운영의 모든 필수 설정을 자동 완성하는 파일은 아닙니다.

실제 모델 호출에는 외부 비용이 발생할 수 있습니다. `rag` 프로필을 켜는 것만으로 전체 서비스의 외부 연동이 격리되는 것은 아닙니다. 이번에는 bootRun, 실서비스 API, Qdrant 색인, Docker 이미지 build/run을 수행하지 않았습니다.

2026 변경으로 고정 JWT 기본값을 제거하고 `JWT_SECRET` 환경변수로 바꾸었으며, 더미 사용자 자동 생성을 `local-fixture` profile로 제한했습니다. 명시적으로 이 profile을 켜면 합성 데이터 계정이 생성되므로 공개 운영 환경에 사용하지 않습니다. 인증·보관·삭제·배포 보안은 별도 운영 검토 대상입니다.

## Docker

원 Dockerfile의 시스템 Gradle8.5를 원 wrapper9.3.0 실행으로 일치시키고 `dependencies || true`를 제거했습니다. `bootJar` 결과를 사용합니다. 이미지 빌드는 미검증이며 빌드 통과를 Docker 실행 통과로 확대하지 않습니다.

## 초기 Streamlit

`legacy/streamlit-prototype`는 초기 소스 보존용입니다. 내부 import 경로를 최소 복구했고 Python syntax를 검사했습니다. 원 requirements/이미지/인덱스 데이터와 실행 환경은 완전 복구되지 않았으며 UI·음성·Pinecone 통합 실행은 미검증입니다. [해당 README](../legacy/streamlit-prototype/README.md)를 먼저 읽습니다.

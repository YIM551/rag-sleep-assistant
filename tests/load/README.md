# 안전한 로컬 부하 시나리오

`local-rag.js`는 2026년 추가한 k6 시나리오입니다. 실제 서비스 동시 사용자 성능 결과는 없습니다. 이번 작업에서는 외부 API 요청을 실행하지 않았습니다.

기본 실행은 HTTP를 보내지 않는 dry-run입니다.

```bash
k6 run tests/load/local-rag.js
```

실제 요청을 보내려면 **OpenAI/embedding/VectorStore를 로컬 테스트 대역으로 교체한 서버**를 준비해야 합니다. localhost 서버도 유료 API를 호출할 수 있습니다. 이 저장소의 Java 단위 테스트 대역은 테스트 코드 안에만 있으며 k6용 HTTP 서버를 자동 제공하지 않습니다.

```bash
# Stub backend 준비 후에만 실행. 인증이 필요하면 토큰을 환경변수로 전달합니다.
k6 run -e ALLOW_LOCAL_LOAD=yes -e LOCAL_BACKEND_USES_STUBS=yes \
  -e BASE_URL=http://127.0.0.1:8080 -e VUS=1 -e ITERATIONS=3 \
  --summary-export=local-summary.json tests/load/local-rag.js
```

상한은 VUS 3, 전체 iteration 10, 요청 timeout 15초, 전체 60초이며 원격 host·redirect·인덱싱 API를 허용하지 않습니다. k6 summary의 `http_reqs`, `http_req_failed`, `checks`, `http_req_duration` 평균/p95를 같이 확인합니다. 이는 테스트 대역 계약/동시 호출 관찰이며 운영 성능이나 실제 LLM 처리량을 나타내지 않습니다.

실행 설정은 k6 공식 [shared-iterations executor](https://grafana.com/docs/k6/latest/using-k6/scenarios/executors/shared-iterations/)의 scenario 구조를 따릅니다. 전체 iteration을 여러 VU가 나누어 실행하므로 VU별 요청 수는 같지 않을 수 있습니다.

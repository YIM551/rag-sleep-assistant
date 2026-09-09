package com.sleepwell.sleepwell_backend.rag.dto;

import java.util.Map;

/**
 * RAG 질의 요청 DTO
 *
 * - namespace: 특정 논문/도메인으로 제한하고 싶을 때 사용 (null 이면 전체)
 * - query : 유저 질문
 * - topK : 상위 몇 개 문서를 가져올지 (null 이면 서비스에서 기본값 사용)
 * - filters : 메타데이터 필터 (예: {"year": ">=2015"})
 * - profile : 사용자 프로필 (성별/나이 등, 선택)
 * - stream : 스트리밍 모드 여부 (지금은 false 고정으로 사용 가능)
 */
public record RagQueryRequest(
    String namespace,
    String query,
    Integer topK,
    Map<String, String> filters,
    Profile profile,
    Boolean stream) {

  /**
   * 가장 단순한 형태의 RAG 요청 생성용 헬퍼
   * - namespace, filters, profile, stream 은 기본값으로 채움
   */
  public static RagQueryRequest simple(String query, Integer topK) {
    return new RagQueryRequest(
        null, // namespace
        query,
        topK,
        null, // filters
        null, // profile
        Boolean.FALSE // stream
    );
  }

  /**
   * 필터까지 포함해서 생성하고 싶을 때 사용하는 헬퍼
   */
  public static RagQueryRequest withFilters(
      String query,
      Integer topK,
      Map<String, String> filters) {
    return new RagQueryRequest(
        null, // namespace
        query,
        topK,
        filters,
        null,
        Boolean.FALSE);
  }

  /**
   * 향후 사용자 프로필을 더 활용하고 싶을 때를 위한 구조
   */
  public record Profile(
      String name,
      Integer age,
      String sex, // female | male | other | unspecified
      String occupation,
      Map<String, Object> extras) {
  }
}
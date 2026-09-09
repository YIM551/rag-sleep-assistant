package com.sleepwell.sleepwell_backend.security.oauth2;

import java.util.Map;

/**
 * OAuth2 사용자 정보 추상화 인터페이스
 * 
 * 다양한 OAuth2 제공자(Google, Kakao, Naver 등)로부터 받은 사용자 정보를
 * 통일된 형태로 추상화하여 처리할 수 있도록 하는 인터페이스입니다.
 * 
 * 각 OAuth2 제공자는 서로 다른 형태의 사용자 정보를 제공하므로,
 * 이를 표준화된 방식으로 접근할 수 있도록 합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * OAuth2 제공자에서 제공하는 고유 ID 반환
     * @return 제공자별 고유 사용자 ID
     */
    public abstract String getId();
    
    /**
     * 사용자 이메일 주소 반환
     * @return 사용자 이메일
     */
    public abstract String getEmail();
    
    /**
     * 사용자 이름 반환
     * @return 사용자 이름
     */
    public abstract String getName();
    
    /**
     * 사용자 프로필 이미지 URL 반환
     * @return 프로필 이미지 URL (없으면 null)
     */
    public abstract String getImageUrl();
} 
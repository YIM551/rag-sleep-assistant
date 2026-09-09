package com.sleepwell.sleepwell_backend.security.oauth2;

import java.util.Map;

/**
 * Google OAuth2 사용자 정보 구현체
 * 
 * Google OAuth2 API로부터 받은 사용자 정보를 처리하는 구현체입니다.
 * Google은 다음과 같은 형태의 사용자 정보를 제공합니다:
 * 
 * {
 *   "sub": "110169484474386276334",
 *   "name": "John Doe",
 *   "given_name": "John",
 *   "family_name": "Doe",
 *   "picture": "https://lh3.googleusercontent.com/a/...",
 *   "email": "portfolio@example.com",
 *   "email_verified": true,
 *   "locale": "en"
 * }
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public class GoogleOAuth2UserInfo extends OAuth2UserInfo {
    
    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }
    
    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }
    
    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }
    
    @Override
    public String getName() {
        String name = (String) attributes.get("name");
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        
        // name이 없는 경우 given_name과 family_name을 조합
        String givenName = (String) attributes.get("given_name");
        String familyName = (String) attributes.get("family_name");
        
        if (givenName != null && familyName != null) {
            return givenName + " " + familyName;
        } else if (givenName != null) {
            return givenName;
        } else if (familyName != null) {
            return familyName;
        }
        
        // 마지막 대안으로 이메일의 로컬 부분 사용
        String email = getEmail();
        if (email != null && email.contains("@")) {
            return email.split("@")[0];
        }
        
        return "Unknown User";
    }
    
    @Override
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    /**
     * 이메일 인증 여부 확인
     * @return 이메일이 인증되었으면 true, 아니면 false
     */
    public Boolean isEmailVerified() {
        Object verified = attributes.get("email_verified");
        if (verified instanceof Boolean) {
            return (Boolean) verified;
        }
        if (verified instanceof String) {
            return Boolean.parseBoolean((String) verified);
        }
        return false;
    }
    
    /**
     * 사용자의 로케일 정보 반환
     * @return 로케일 (예: "en", "ko")
     */
    public String getLocale() {
        return (String) attributes.get("locale");
    }
} 
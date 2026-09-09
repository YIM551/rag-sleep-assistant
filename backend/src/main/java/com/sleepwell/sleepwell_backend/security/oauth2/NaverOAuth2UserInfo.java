package com.sleepwell.sleepwell_backend.security.oauth2;

import java.util.Map;

public class NaverOAuth2UserInfo extends OAuth2UserInfo {

    private Map<String, Object> responseAttributes;

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
        if (attributes.containsKey("response")) {
            this.responseAttributes = (Map<String, Object>) attributes.get("response");
        } else {
            this.responseAttributes = attributes;
        }
    }

    @Override
    public String getId() {
        return (String) responseAttributes.get("id");
    }

    @Override
    public String getName() {
        return (String) responseAttributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) responseAttributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) responseAttributes.get("profile_image");
    }
} 
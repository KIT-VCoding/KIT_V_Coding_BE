package com.example.thinkmap.security.oauth2.userinfo;

import com.example.thinkmap.domain.entity.AuthProvider;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId,
                                                   Map<String, Object> attributes) {
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
        return switch (provider) {
            case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
            case KAKAO  -> new KakaoOAuth2UserInfo(attributes);
        };
    }
}

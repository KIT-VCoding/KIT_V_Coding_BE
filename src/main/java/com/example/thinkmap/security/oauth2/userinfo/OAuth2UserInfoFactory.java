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
            case LOCAL  -> throw new IllegalArgumentException("자체 로그인은 OAuth2를 사용하지 않습니다.");
        };
    }
}

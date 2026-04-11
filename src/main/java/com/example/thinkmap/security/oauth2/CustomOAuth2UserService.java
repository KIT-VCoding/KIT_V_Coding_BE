package com.example.thinkmap.security.oauth2;

import com.example.thinkmap.domain.entity.AuthProvider;
import com.example.thinkmap.domain.entity.User;
import com.example.thinkmap.domain.repository.UserRepository;
import com.example.thinkmap.security.UserPrincipal;
import com.example.thinkmap.security.oauth2.userinfo.OAuth2UserInfo;
import com.example.thinkmap.security.oauth2.userinfo.OAuth2UserInfoFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId, oAuth2User.getAttributes());

        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
        User user = userRepository
                .findByProviderAndProviderId(provider, userInfo.getId())
                .map(existing -> {
                    existing.update(userInfo.getName(), userInfo.getImageUrl());
                    return existing;
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .name(userInfo.getName())
                                .email(userInfo.getEmail())
                                .profileImageUrl(userInfo.getImageUrl())
                                .provider(provider)
                                .providerId(userInfo.getId())
                                .build()
                ));

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }
}

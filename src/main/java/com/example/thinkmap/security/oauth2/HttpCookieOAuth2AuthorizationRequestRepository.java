package com.example.thinkmap.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookieValue(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE)
                .map(this::deserialize)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE);
            deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }
        addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE,
                serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);

        String redirectUri = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (org.springframework.util.StringUtils.hasText(redirectUri)) {
            addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUri, COOKIE_EXPIRE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest req = null;
        try {
            req = loadAuthorizationRequest(request);
        } catch (Exception ignored) {}
        deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE);
        return req;
    }

    // ── 쿠키 유틸 ────────────────────────────────────────────────

    private java.util.Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return java.util.Optional.empty();
        for (Cookie c : cookies) {
            if (c.getName().equals(name)) return java.util.Optional.of(c.getValue());
        }
        return java.util.Optional.empty();
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        for (Cookie c : cookies) {
            if (c.getName().equals(name)) {
                c.setValue("");
                c.setPath("/");
                c.setMaxAge(0);
                response.addCookie(c);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private String serialize(Object object) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
    }

    @SuppressWarnings("deprecation")
    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            if (!org.springframework.util.StringUtils.hasText(value)) return null;
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            if (bytes.length == 0) return null;
            return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(bytes);
        } catch (Exception e) {
            return null;
        }
    }
}

package com.connectsphere.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoogleAccountChooserAuthorizationRequestResolverTest {

    @Test
    void resolve_googleRequest_addsSelectAccountPrompt() {
        GoogleAccountChooserAuthorizationRequestResolver resolver =
                new GoogleAccountChooserAuthorizationRequestResolver(
                        new InMemoryClientRegistrationRepository(googleRegistration()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        OAuth2AuthorizationRequest authorizationRequest = resolver.resolve(request, "google");

        assertEquals("select_account", authorizationRequest.getAdditionalParameters().get("prompt"));
    }

    private ClientRegistration googleRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId("google-client")
                .clientSecret("google-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .scope("profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
    }

}

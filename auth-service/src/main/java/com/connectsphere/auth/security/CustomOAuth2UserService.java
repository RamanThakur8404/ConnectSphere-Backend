package com.connectsphere.auth.security;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.connectsphere.auth.config.RabbitMQConfig;
import com.connectsphere.auth.constant.AuthProvider;
import com.connectsphere.auth.constant.Role;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.event.UserRegisteredEvent;
import com.connectsphere.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;
    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        return processOAuth2User(registrationId, oAuth2User);
    }

    OAuth2User processOAuth2User(String providerString, OAuth2User oAuth2User) {
    	 // Resolve provider enum (fall back to LOCAL if unknown — should not happen)
        AuthProvider provider;
        try {
            provider = AuthProvider.valueOf(providerString.toUpperCase());
        } catch (IllegalArgumentException e) {
            provider = AuthProvider.LOCAL;
        }
        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            throw new RuntimeException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Link account to provider if it was previously unset
            if (!provider.equals(user.getProvider())) {
                user.setProvider(provider);
            }
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
            }
            user = userRepository.save(user);
        } else {
            String name = oAuth2User.getAttribute("name");
            String username = email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5);

            user = new User();
            user.setEmail(email);
            user.setUsername(username.substring(0, Math.min(username.length(), 50)));
            user.setFullName(name != null ? (name.length() > 100 ? name.substring(0, 100) : name) : "");
            user.setProvider(provider);
            user.setRole(Role.USER);
            user.setActive(true);
            user.setEmailVerified(true);
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString())); // dummy constraint satisfier

            String avatarUrl = oAuth2User.getAttribute("picture");
            user.setProfilePicUrl(avatarUrl != null && avatarUrl.length() <= 500 ? avatarUrl : "");

            user = userRepository.save(user);
            log.info("Created new user from OAuth2. Email: {}, Provider: {}", email, provider);
            publishUserRegisteredEvent(user);
        }

        return new CustomOAuth2User(oAuth2User, user);
    }

    private void publishUserRegisteredEvent(User user) {
        try {
            UserRegisteredEvent event = UserRegisteredEvent.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .build();
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.AUTH_EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_USER_REGISTERED,
                    event);
            log.info("Published UserRegisteredEvent for OAuth2 userId: {}", user.getUserId());
        } catch (Exception ex) {
            // Keep OAuth login successful even if the notification event cannot be published.
            log.error("Failed to publish OAuth2 UserRegisteredEvent: {}", ex.getMessage(), ex);
        }
    }
}

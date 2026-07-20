package org.example.projecttcg.service;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.User;
import org.example.projecttcg.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        // Automatically register the user if they do not exist
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .password("") // empty password for OAuth2 users
                    .displayName(name != null ? name : "Google User")
                    .avatarUrl(picture)
                    .role(User.Role.MEMBER)
                    .status(User.UserStatus.ACTIVE)
                    .balance(500000.0) // Seed balance for initial testing
                    .build();
            return userRepository.save(newUser);
        });

        // Update avatar url if they already exist but have no avatar
        if (picture != null && (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty())) {
            user.setAvatarUrl(picture);
            userRepository.save(user);
        }

        return oAuth2User;
    }
}

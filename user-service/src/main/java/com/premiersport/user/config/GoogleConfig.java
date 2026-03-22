package com.premiersport.user.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Slf4j
@Configuration
public class GoogleConfig {

    @Value("${google.client-id:}")
    private String googleClientId;

    @Bean
    @ConditionalOnExpression("!'${google.client-id:}'.isEmpty()")
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        log.info("Google OAuth configured with client ID: {}...", googleClientId.substring(0, Math.min(8, googleClientId.length())));
        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(googleClientId))
            .build();
    }
}

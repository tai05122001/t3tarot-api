package vn.t3tarot.api.identity.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityConfig {

    @Bean
    PasswordEncoder passwordEncoder(IdentityProperties properties) {
        return new BCryptPasswordEncoder(properties.passwordHashStrength());
    }
}

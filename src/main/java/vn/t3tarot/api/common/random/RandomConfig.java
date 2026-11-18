package vn.t3tarot.api.common.random;

import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class RandomConfig {

    @Bean
    SecureRandom secureRandom() {
        return new SecureRandom();
    }
}

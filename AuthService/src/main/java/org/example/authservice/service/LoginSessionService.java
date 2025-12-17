package org.example.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LoginSessionService {
    private final StringRedisTemplate redis;
    private final Duration ttl;

    public LoginSessionService(
            StringRedisTemplate redis,
            @Value("${login-session.ttl-seconds:300}") long ttlSeconds
    ) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public void createSession(String sub) {
        redis.opsForValue().set(key(sub), "1", ttl);
    }

    public boolean isSessionAlive(String sub) {
        Boolean exists = redis.hasKey(key(sub));
        return exists != null && exists;
    }

    public void deleteSession(String sub) {
        redis.delete(key(sub));
    }

    private String key(String sub) {
        return "login:session:" + sub;
    }
}

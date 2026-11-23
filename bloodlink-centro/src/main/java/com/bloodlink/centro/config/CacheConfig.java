package com.bloodlink.centro.config;

import com.bloodlink.common.cache.CacheJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class CacheConfig {

    @Bean
    CacheJson cacheJson(StringRedisTemplate redis, ObjectMapper mapper) {
        return new CacheJson(redis, mapper);
    }
}

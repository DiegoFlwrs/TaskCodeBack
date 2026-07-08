package com.flores.taskcodeback.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(name = "app.cache.redis.enabled", havingValue = "true")
    public CacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            @Value("${app.cache.ttl.team-stats-minutes:10}") long teamStatsTtl,
            @Value("${app.cache.ttl.lists-minutes:3}") long listsTtl) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(listsTtl))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(CacheNames.TEAM_STATS, defaults.entryTtl(Duration.ofMinutes(teamStatsTtl)));
        perCache.put(CacheNames.TEAMS, defaults.entryTtl(Duration.ofMinutes(5)));
        perCache.put(CacheNames.TEAM_MEMBERS, defaults.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.cache.redis.enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager caffeineCacheManager(
            @Value("${app.cache.ttl.lists-minutes:3}") long listsTtl) {

        CaffeineCacheManager manager = new CaffeineCacheManager(
                CacheNames.TASKS,
                CacheNames.TICKETS,
                CacheNames.APPS,
                CacheNames.TEAMS,
                CacheNames.TEAM_STATS,
                CacheNames.TEAM_MEMBERS
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(listsTtl, TimeUnit.MINUTES)
                .maximumSize(1_000));
        return manager;
    }
}

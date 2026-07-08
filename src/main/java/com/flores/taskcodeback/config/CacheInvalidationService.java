package com.flores.taskcodeback.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationService {

    private final CacheManager cacheManager;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public void evictUserData(String email) {
        evictByEmailPrefix(CacheNames.TASKS, email);
        evictByEmailPrefix(CacheNames.TICKETS, email);
        evictByEmailPrefix(CacheNames.APPS, email);
        evictByEmailPrefix(CacheNames.TEAMS, email);
        evictByEmailPrefix(CacheNames.TEAM_MEMBERS, email);
        evictAll(CacheNames.TEAM_STATS);
    }

    public void evictTasks(String email) {
        evictByEmailPrefix(CacheNames.TASKS, email);
        evictAll(CacheNames.TEAM_STATS);
    }

    public void evictTickets(String email) {
        evictByEmailPrefix(CacheNames.TICKETS, email);
        evictAll(CacheNames.TEAM_STATS);
    }

    public void evictApps(String email) {
        evictByEmailPrefix(CacheNames.APPS, email);
    }

    public void evictTeams(String email) {
        evictByEmailPrefix(CacheNames.TEAMS, email);
        evictByEmailPrefix(CacheNames.TEAM_MEMBERS, email);
        evictAll(CacheNames.TEAM_STATS);
    }

    public void evictTeamStats() {
        evictAll(CacheNames.TEAM_STATS);
    }

    public void evictAllTeamMembers() {
        evictAll(CacheNames.TEAM_MEMBERS);
    }

    private void evictByEmailPrefix(String cacheName, String email) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return;
        }
        String prefix = email + ":";
        if (cache instanceof CaffeineCache caffeineCache) {
            caffeineCache.getNativeCache().asMap().keySet().removeIf(key ->
                    Objects.toString(key, "").startsWith(prefix));
            return;
        }
        if (redisTemplate != null) {
            String pattern = cacheName + "::" + email + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            String exactKey = cacheName + "::" + email;
            redisTemplate.delete(exactKey);
        }
    }

    private void evictAll(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Cache cleared: {}", cacheName);
        }
    }
}

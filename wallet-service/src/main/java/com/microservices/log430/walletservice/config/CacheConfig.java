package com.microservices.log430.walletservice.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {

    @Bean
    @Override
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Configuration pour le cache des portefeuilles
        // TTL: 30 secondes (données changeantes mais tolérance acceptable)
        cacheManager.registerCustomCache("walletCache",
            Caffeine.newBuilder()
                .maximumSize(1000)  // Max 1000 portefeuilles en cache
                .expireAfterWrite(30, TimeUnit.SECONDS)  // TTL 30 secondes
                .recordStats()  // Active les métriques pour monitoring
                .build());

        // Configuration pour le cache des stocks
        // TTL: 2 minutes (données de référence, changent moins souvent)
        cacheManager.registerCustomCache("stockCache",
            Caffeine.newBuilder()
                .maximumSize(500)   // Max 500 symboles en cache
                .expireAfterWrite(2, TimeUnit.MINUTES)  // TTL 2 minutes
                .recordStats()  // Active les métriques pour monitoring
                .build());

        return cacheManager;
    }
}

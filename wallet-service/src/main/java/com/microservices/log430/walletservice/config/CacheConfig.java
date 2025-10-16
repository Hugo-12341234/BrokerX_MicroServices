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
        // TTL: 1 heure (très élevé car invalidation manuelle via @CacheEvict)
        // Stratégie: Cache invalidé uniquement lors de modifications réelles du portefeuille
        // Avantage: Maximise les cache hits, réduit la charge sur la DB
        cacheManager.registerCustomCache("walletCache",
            Caffeine.newBuilder()
                .maximumSize(1000)  // Max 1000 portefeuilles en cache
                .expireAfterWrite(1, TimeUnit.HOURS)  // TTL 1 heure (très élevé)
                .recordStats()  // Active les métriques pour monitoring
                .build());

        // Configuration pour le cache des stocks
        // TTL: 2 minutes (données de référence, changent moins souvent)
        cacheManager.registerCustomCache("stockCache",
            Caffeine.newBuilder()
                .maximumSize(500)   // Max 500 symboles en cache
                .expireAfterWrite(1, TimeUnit.HOURS)  // TTL 2 minutes
                .recordStats()  // Active les métriques pour monitoring
                .build());

        return cacheManager;
    }
}

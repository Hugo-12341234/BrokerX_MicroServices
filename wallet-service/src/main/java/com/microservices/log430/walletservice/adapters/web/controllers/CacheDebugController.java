package com.microservices.log430.walletservice.adapters.web.controllers;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug")
public class CacheDebugController {

    private final CacheManager cacheManager;

    public CacheDebugController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Endpoint pour voir exactement ce qui est en cache
     */
    @GetMapping("/cache")
    public ResponseEntity<Map<String, Object>> getCacheContents(@RequestParam("cacheName") String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        Map<String, Object> result = new HashMap<>();

        if (cache == null) {
            result.put("error", "Cache '" + cacheName + "' non trouvé ou pas encore initialisé");
            result.put("suggestion", "Le cache sera créé lors du premier élément ajouté");
            result.put("availableCaches", cacheManager.getCacheNames());
            return ResponseEntity.ok(result);
        }

        if (cache instanceof CaffeineCache) {
            CaffeineCache caffeineCache = (CaffeineCache) cache;
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();

            // Stats du cache
            CacheStats stats = nativeCache.stats();
            result.put("stats", Map.of(
                "hitCount", stats.hitCount(),
                "missCount", stats.missCount(),
                "hitRate", String.format("%.2f%%", stats.hitRate() * 100),
                "size", nativeCache.estimatedSize()
            ));

            // Contenu du cache (limité pour éviter trop de données)
            Map<String, Object> cacheContents = new HashMap<>();
            nativeCache.asMap().forEach((key, value) -> {
                cacheContents.put(key.toString(), Map.of(
                    "type", value.getClass().getSimpleName(),
                    "value", value.toString().substring(0, Math.min(200, value.toString().length())) + "..."
                ));
            });
            result.put("contents", cacheContents);

        } else {
            result.put("error", "Cache type not supported for inspection");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint pour vider un cache spécifique
     */
    @DeleteMapping("/cache")
    public ResponseEntity<String> clearCache(@RequestParam("cacheName") String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return ResponseEntity.ok("Cache " + cacheName + " vidé avec succès");
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Endpoint pour voir une entrée spécifique du cache
     */
    @GetMapping("/cache/entry")
    public ResponseEntity<Map<String, Object>> getCacheEntry(@RequestParam("cacheName") String cacheName, @RequestParam("key") String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                Object value = wrapper.get();
                return ResponseEntity.ok(Map.of(
                    "key", key,
                    "type", value.getClass().getSimpleName(),
                    "value", value,
                    "cached", true
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "key", key,
                    "cached", false,
                    "message", "Clé non trouvée en cache"
                ));
            }
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Endpoint pour vérifier que Spring trouve bien nos caches configurés
     */
    @GetMapping("/cache/info")
    public ResponseEntity<Map<String, Object>> getCacheInfo() {
        Cache cache = cacheManager.getCache("walletCache");
        Map<String, Object> result = new HashMap<>();

        if (cache == null) {
            result.put("cacheFound", false);
            result.put("message", "❌ Cache walletCache non trouvé ou pas encore initialisé");
            result.put("availableCaches", cacheManager.getCacheNames());
            return ResponseEntity.ok(result);
        }

        if (cache instanceof CaffeineCache) {
            CaffeineCache caffeineCache = (CaffeineCache) cache;
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();

            // Preuve que Spring trouve bien ta config
            result.put("cacheFound", true);
            result.put("cacheType", cache.getClass().getSimpleName());
            result.put("nativeCacheType", nativeCache.getClass().getSimpleName());

            // Stats pour prouver que recordStats() fonctionne
            com.github.benmanes.caffeine.cache.stats.CacheStats stats = nativeCache.stats();
            result.put("stats", Map.of(
                "hitCount", stats.hitCount(),
                "missCount", stats.missCount(),
                "size", nativeCache.estimatedSize()
            ));

            result.put("message", "✅ Spring a trouvé et configuré le cache walletCache avec Caffeine !");
        } else {
            result.put("cacheFound", false);
            result.put("message", "❌ Cache walletCache non trouvé ou mal configuré");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint de test pour observer le comportement du TTL et des stats
     */
    @GetMapping("/test/ttl")
    public ResponseEntity<Map<String, Object>> testTTLBehavior() {
        Map<String, Object> result = new HashMap<>();

        // Infos générales sur le cache
        Cache cache = cacheManager.getCache("walletCache");
        if (cache instanceof CaffeineCache) {
            CaffeineCache caffeineCache = (CaffeineCache) cache;
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();

            // Stats actuelles (JAMAIS reset par le TTL)
            CacheStats stats = nativeCache.stats();
            result.put("cacheStats", Map.of(
                "hitCount", stats.hitCount(),
                "missCount", stats.missCount(),
                "hitRate", String.format("%.2f%%", stats.hitRate() * 100),
                "evictionCount", stats.evictionCount()  // Nombre d'expirations TTL
            ));

            // Contenu actuel du cache
            result.put("currentSize", nativeCache.estimatedSize());
            result.put("entriesInCache", nativeCache.asMap().keySet().toString());

            result.put("ttlConfig", "1 hour (3600 seconds)");
            result.put("explanation", Map.of(
                "strategy", "Cache invalidé UNIQUEMENT par @CacheEvict lors de modifications",
                "ttlBehavior", "Chaque entrée expire 1h après sa création (sécurité)",
                "primaryInvalidation", "@CacheEvict sur POST /update quand portefeuille modifié",
                "advantage", "Maximise cache hits - pas d'expiration inutile",
                "statsPreserved", "Les stats (hit/miss count) ne sont JAMAIS reset"
            ));
        }

        return ResponseEntity.ok(result);
    }
}

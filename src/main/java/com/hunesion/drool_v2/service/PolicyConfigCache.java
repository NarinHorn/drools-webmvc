package com.hunesion.drool_v2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * PolicyConfigCache - Caches parsed policy config JSON to avoid repeated parsing
 * This significantly improves performance when the same policies are accessed repeatedly
 */
@Service
public class PolicyConfigCache {

    private final ObjectMapper objectMapper;

    public PolicyConfigCache(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse and cache policy config JSON
     * Cache key: policy ID
     * 
     * @param policyId The policy ID (used as cache key)
     * @param policyConfigJson The JSON string to parse
     * @return Parsed Map representation of the policy config
     */
    @Cacheable(value = "policyConfigCache", key = "#policyId")
    public Map<String, Object> getParsedConfig(Long policyId, String policyConfigJson) {
        if (policyConfigJson == null || policyConfigJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(policyConfigJson, Map.class);
            return config;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse policy config for policy: " + policyId, e);
        }
    }

    /**
     * Evict cache when policy is updated
     * 
     * @param policyId The policy ID to evict from cache
     */
    @CacheEvict(value = "policyConfigCache", key = "#policyId")
    public void evictPolicyConfig(Long policyId) {
        // Cache eviction handled by annotation
    }

    /**
     * Evict all policy configs (when rebuilding rules)
     */
    @CacheEvict(value = "policyConfigCache", allEntries = true)
    public void evictAllPolicyConfigs() {
        // Cache eviction handled by annotation
    }
}

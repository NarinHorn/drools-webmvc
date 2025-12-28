package com.hunesion.drool_v2.config;

/**
 * DroolsConfig - DEPRECATED
 * 
 * This configuration has been replaced by DynamicRuleService which provides:
 * - Dynamic rule loading from database
 * - Hot-reloading of rules when policies change
 * - Thread-safe KieSession creation
 * 
 * See: com.hunesion.drool_v2.service.DynamicRuleService
 */
// @Configuration - Disabled in favor of DynamicRuleService
public class DroolsConfig {
    // Kept for reference - functionality moved to DynamicRuleService
}

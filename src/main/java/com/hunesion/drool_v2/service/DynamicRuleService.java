package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.model.entity.AccessPolicy;
import com.hunesion.drool_v2.repository.AccessPolicyRepository;
import jakarta.annotation.PostConstruct;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * DynamicRuleService - Manages KieContainer lifecycle for dynamic rule loading
 * 
 * This service:
 * - Loads static rules from classpath on startup
 * - Loads dynamic policies from database
 * - Provides thread-safe access to KieSession
 * - Supports hot-reloading of rules when policies change
 */
@Service
public class DynamicRuleService {

    private final AccessPolicyRepository accessPolicyRepository;
    private final KieServices kieServices;
    private volatile KieContainer kieContainer;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static final String STATIC_RULES_PATH = "rules/";
    private static final String DYNAMIC_RULES_PATH = "src/main/resources/rules/dynamic/";

    @Autowired
    public DynamicRuleService(AccessPolicyRepository accessPolicyRepository) {
        this.accessPolicyRepository = accessPolicyRepository;
        this.kieServices = KieServices.Factory.get();
    }

    @Autowired
    private EquipmentPolicyRuleGenerator equipmentPolicyRuleGenerator;

    private void loadEquipmentPoliciesFromDatabase(KieFileSystem kieFileSystem) {
        try {
            String equipmentRules = equipmentPolicyRuleGenerator.generateAllPolicyRules();
            if (equipmentRules != null && !equipmentRules.trim().isEmpty()) {
                kieFileSystem.write(DYNAMIC_RULES_PATH + "equipment-policies.drl", equipmentRules);
                System.out.println("  Loaded equipment policies from database");
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load equipment policies - " + e.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        rebuildRules();
    }

    /**
     * Rebuilds the KieContainer with all rules (static + dynamic from DB)
     * Called when policies are created, updated, or deleted
     */
    public void rebuildRules() {
        lock.writeLock().lock();
        try {
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

            // Load static rules from classpath
            loadStaticRules(kieFileSystem);

            // Load dynamic rules from database
            loadDynamicRulesFromDatabase(kieFileSystem);

            // Load equipment policies from database
            loadEquipmentPoliciesFromDatabase(kieFileSystem);

            // Build and verify
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();

            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                throw new RuntimeException("Drools rule compilation errors:\n" 
                        + kieBuilder.getResults().getMessages());
            }

            // Create new container
            this.kieContainer = kieServices.newKieContainer(
                    kieBuilder.getKieModule().getReleaseId()
            );

            System.out.println("✓ Rules rebuilt successfully");
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadStaticRules(KieFileSystem kieFileSystem) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:" + STATIC_RULES_PATH + "**/*.drl");
            
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                // Skip dynamic access policy rules that should come from DB
                if (filename != null && !filename.startsWith("access-policy")) {
                    String path = STATIC_RULES_PATH + filename;
                    kieFileSystem.write("src/main/resources/" + path,
                            kieServices.getResources().newInputStreamResource(resource.getInputStream()));
                    System.out.println("  Loaded static rule: " + filename);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load static rules - " + e.getMessage());
        }
    }

    private void loadDynamicRulesFromDatabase(KieFileSystem kieFileSystem) {
        List<AccessPolicy> policies = accessPolicyRepository.findByEnabledTrueOrderByPriorityDesc();
        
        if (policies.isEmpty()) {
            System.out.println("  No dynamic policies found in database");
            // Add a default permissive rule
            String defaultRule = generateDefaultRule();
            kieFileSystem.write(DYNAMIC_RULES_PATH + "default-policy.drl", defaultRule);
            return;
        }

        // Combine all policy DRLs into one file
        StringBuilder combinedDrl = new StringBuilder();
        combinedDrl.append("package rules.dynamic;\n\n");
        combinedDrl.append("import com.hunesion.drool_v2.model.AccessRequest;\n");
        combinedDrl.append("import com.hunesion.drool_v2.model.AccessResult;\n\n");

        for (AccessPolicy policy : policies) {
            if (policy.getGeneratedDrl() != null && !policy.getGeneratedDrl().isEmpty()) {
                // Extract just the rule part (without package and imports)
                String drl = policy.getGeneratedDrl();
                int ruleStart = drl.indexOf("rule ");
                if (ruleStart >= 0) {
                    combinedDrl.append(drl.substring(ruleStart));
                    combinedDrl.append("\n\n");
                }
                System.out.println("  Loaded dynamic policy: " + policy.getPolicyName());
            }
        }

        kieFileSystem.write(DYNAMIC_RULES_PATH + "access-policies.drl", combinedDrl.toString());
    }

    private String generateDefaultRule() {
        return """
            package rules.dynamic;
            
            import com.hunesion.drool_v2.model.AccessRequest;
            import com.hunesion.drool_v2.model.AccessResult;
            
            // Default rule: Deny all if no other rules match (evaluated last due to low salience)
            rule "Default Deny All"
                salience -1000
                when
                    $request : AccessRequest()
                    $result : AccessResult(evaluated == false)
                then
                    $result.deny("Default Deny All", "No matching policy found");
                    System.out.println("Default Deny rule applied for: " + $request.getEndpoint());
            end
            """;
    }

    /**
     * Creates a new KieSession for rule evaluation
     * Thread-safe: uses read lock to allow concurrent sessions
     */
    public KieSession newKieSession() {
        lock.readLock().lock();
        try {
            return kieContainer.newKieSession();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the current KieContainer (for advanced usage)
     */
    public KieContainer getKieContainer() {
        lock.readLock().lock();
        try {
            return kieContainer;
        } finally {
            lock.readLock().unlock();
        }
    }
}

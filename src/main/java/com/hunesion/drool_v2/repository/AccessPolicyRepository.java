package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.model.entity.AccessPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessPolicyRepository extends JpaRepository<AccessPolicy, Long> {
    Optional<AccessPolicy> findByPolicyName(String policyName);
    List<AccessPolicy> findByEnabledTrueOrderByPriorityDesc();
    List<AccessPolicy> findByEndpointAndHttpMethod(String endpoint, String httpMethod);
    boolean existsByPolicyName(String policyName);
}

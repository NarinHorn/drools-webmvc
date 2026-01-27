package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.model.entity.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyTypeRepository extends JpaRepository<PolicyType, Long> {
    Optional<PolicyType> findByTypeCode(String typeCode);
    List<PolicyType> findByActiveTrue();
    Optional<PolicyType> findByTypeCodeAndActiveTrue(String typeCode);
}

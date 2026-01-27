package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.model.entity.EquipmentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentGroupRepository extends JpaRepository<EquipmentGroup, Long> {
    Optional<EquipmentGroup> findByGroupName(String groupName);
    boolean existsByGroupName(String groupName);
}


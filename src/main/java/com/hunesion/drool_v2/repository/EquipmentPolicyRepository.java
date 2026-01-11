package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.model.entity.EquipmentPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentPolicyRepository extends JpaRepository<EquipmentPolicy, Long> {
    Optional<EquipmentPolicy> findByPolicyName(String policyName);
    List<EquipmentPolicy> findByEnabledTrueOrderByPriorityDesc();
    boolean existsByPolicyName(String policyName);

    @Query("SELECT DISTINCT ep FROM EquipmentPolicy ep " +
            "JOIN ep.userAssignments ua WHERE ua.user.id = :userId AND ep.enabled = true")
    List<EquipmentPolicy> findAssignedToUser(@Param("userId") Long userId);

    @Query("SELECT DISTINCT ep FROM EquipmentPolicy ep " +
            "JOIN ep.groupAssignments ga WHERE ga.group.id = :groupId AND ep.enabled = true")
    List<EquipmentPolicy> findAssignedToGroup(@Param("groupId") Long groupId);

    @Query("SELECT DISTINCT ep FROM EquipmentPolicy ep " +
            "JOIN ep.equipmentAssignments ea WHERE ea.equipment.id = :equipmentId AND ep.enabled = true")
    List<EquipmentPolicy> findAssignedToEquipment(@Param("equipmentId") Long equipmentId);

    @Query("SELECT DISTINCT ep FROM EquipmentPolicy ep " +
            "JOIN ep.roleAssignments ra WHERE ra.role.id = :roleId AND ep.enabled = true")
    List<EquipmentPolicy> findAssignedToRole(@Param("roleId") Long roleId);
}
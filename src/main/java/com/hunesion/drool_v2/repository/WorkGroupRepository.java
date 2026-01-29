package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.model.entity.WorkGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkGroupRepository extends JpaRepository<WorkGroup, Long> {

    Optional<WorkGroup> findByWorkGroupName(String workGroupName);

    boolean existsByWorkGroupName(String workGroupName);

    List<WorkGroup> findByEnabledTrue();

    // Find work groups that contain a specific user
    @Query("SELECT DISTINCT wg FROM WorkGroup wg JOIN wg.users u WHERE u.id = :userId AND wg.enabled = true")
    List<WorkGroup> findByUserId(@Param("userId") Long userId);

    // Find work groups that contain a specific equipment
    @Query("SELECT DISTINCT wg FROM WorkGroup wg JOIN wg.equipment e WHERE e.id = :equipmentId AND wg.enabled = true")
    List<WorkGroup> findByEquipmentId(@Param("equipmentId") Long equipmentId);

    // Find work groups that contain both a specific user AND equipment
    @Query("SELECT DISTINCT wg FROM WorkGroup wg " +
           "JOIN wg.users u " +
           "JOIN wg.equipment e " +
           "WHERE u.id = :userId AND e.id = :equipmentId AND wg.enabled = true")
    List<WorkGroup> findByUserIdAndEquipmentId(@Param("userId") Long userId, @Param("equipmentId") Long equipmentId);

    // Find work groups that contain a specific account
    @Query("SELECT DISTINCT wg FROM WorkGroup wg JOIN wg.accounts a WHERE a.id = :accountId AND wg.enabled = true")
    List<WorkGroup> findByAccountId(@Param("accountId") Long accountId);
}

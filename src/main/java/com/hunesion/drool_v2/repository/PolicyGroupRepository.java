package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.model.entity.PolicyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyGroupRepository extends JpaRepository<PolicyGroup, Long> {

    Optional<PolicyGroup> findByGroupName(String groupName);

    boolean existsByGroupName(String groupName);

    List<PolicyGroup> findByEnabledTrue();

    @Query("SELECT DISTINCT pg FROM PolicyGroup pg " +
           "JOIN pg.userAssignments ua WHERE ua.user.id = :userId AND pg.enabled = true")
    List<PolicyGroup> findAssignedToUser(@Param("userId") Long userId);

    @Query("SELECT DISTINCT pg FROM PolicyGroup pg " +
           "JOIN pg.userGroupAssignments uga WHERE uga.userGroup.id = :userGroupId AND pg.enabled = true")
    List<PolicyGroup> findAssignedToUserGroup(@Param("userGroupId") Long userGroupId);

    @Query("SELECT DISTINCT pg FROM PolicyGroup pg " +
           "JOIN pg.roleAssignments ra WHERE ra.role.id = :roleId AND pg.enabled = true")
    List<PolicyGroup> findAssignedToRole(@Param("roleId") Long roleId);
}

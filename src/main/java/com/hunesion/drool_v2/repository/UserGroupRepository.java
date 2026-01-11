package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.model.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    Optional<UserGroup> findByGroupName(String groupName);
    boolean existsByGroupName(String groupName);
}
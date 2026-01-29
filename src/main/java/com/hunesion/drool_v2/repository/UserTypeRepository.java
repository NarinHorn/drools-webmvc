package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.model.entity.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTypeRepository extends JpaRepository<UserType, Long> {
    Optional<UserType> findByTypeCode(String typeCode);
    Optional<UserType> findByTypeCodeAndActiveTrue(String typeCode);
    List<UserType> findByActiveTrue();
    boolean existsByTypeCode(String typeCode);
}

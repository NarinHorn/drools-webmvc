package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.model.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountTypeRepository extends JpaRepository<AccountType, Long> {
    Optional<AccountType> findByTypeCode(String typeCode);
    Optional<AccountType> findByTypeCodeAndActiveTrue(String typeCode);
    List<AccountType> findByActiveTrue();
    boolean existsByTypeCode(String typeCode);
}

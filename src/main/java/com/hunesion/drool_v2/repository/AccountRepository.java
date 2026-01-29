package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.model.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNameAndEquipmentId(String accountName, Long equipmentId);
    List<Account> findByEquipmentId(Long equipmentId);
    List<Account> findByEquipmentIdAndActiveTrue(Long equipmentId);
    List<Account> findByAccountTypeId(Long accountTypeId);
    List<Account> findByAccountTypeIdAndActiveTrue(Long accountTypeId);
    
    @Query("SELECT a FROM Account a WHERE a.equipment.id = :equipmentId AND a.accountType.typeCode = :typeCode AND a.active = true")
    List<Account> findByEquipmentIdAndAccountTypeCode(@Param("equipmentId") Long equipmentId, @Param("typeCode") String typeCode);
    
    boolean existsByAccountNameAndEquipmentId(String accountName, Long equipmentId);
}

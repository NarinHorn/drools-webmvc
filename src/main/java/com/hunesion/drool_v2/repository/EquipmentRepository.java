package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.model.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    
    // Find all non-deleted equipment
    List<Equipment> findByIsDeletedFalse();
    
    // Find equipment by device name (non-deleted)
    Optional<Equipment> findByDeviceNameAndIsDeletedFalse(String deviceName);
    
    // Find equipment by IP address (non-deleted)
    Optional<Equipment> findByIpAddressAndIsDeletedFalse(String ipAddress);
    
    // Check if device name exists (non-deleted)
    boolean existsByDeviceNameAndIsDeletedFalse(String deviceName);
    
    // Soft delete - mark as deleted
    @Modifying
    @Query("UPDATE Equipment e SET e.isDeleted = true, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
    void softDelete(@Param("id") Long id);
    
    // Find by device type (non-deleted)
    List<Equipment> findByDeviceTypeAndIsDeletedFalse(String deviceType);
}

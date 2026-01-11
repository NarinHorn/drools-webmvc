package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.model.entity.CommandList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommandListRepository extends JpaRepository<CommandList, Long> {
    Optional<CommandList> findByListNameAndListTypeAndProtocolType(
            String listName, String listType, String protocolType);
}
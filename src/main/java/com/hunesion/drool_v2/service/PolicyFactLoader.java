package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.model.entity.*;
import com.hunesion.drool_v2.model.EquipmentAccessRequest;
import com.hunesion.drool_v2.model.EquipmentAccessRequest.TimeSlot;
import com.hunesion.drool_v2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PolicyFactLoader - Loads policy data into Drools fact objects
 * This service makes it easy to populate EquipmentAccessRequest with all relevant policy data
 */
@Service
public class PolicyFactLoader {

    private final EquipmentPolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;

    @Autowired
    public PolicyFactLoader(EquipmentPolicyRepository policyRepository,
                            UserRepository userRepository,
                            EquipmentRepository equipmentRepository) {
        this.policyRepository = policyRepository;
        this.userRepository = userRepository;
        this.equipmentRepository = equipmentRepository;
    }

    /**
     * Load all relevant policies for a user and equipment into the request fact
     */
    @Transactional(readOnly = true)
    public EquipmentAccessRequest loadPoliciesIntoFact(String username, Long equipmentId) {
        EquipmentAccessRequest request = new EquipmentAccessRequest();
        request.setUsername(username);
        request.setEquipmentId(equipmentId);

        // Load user data
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        request.setUserId(user.getId());
        request.setUserRoles(user.getRoleNames());
        request.setUserGroups(user.getGroupNames());

        // Load equipment data
        if (equipmentId != null) {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElse(null);
            if (equipment != null) {
                request.setEquipmentName(equipment.getDeviceName());
                request.setProtocol(equipment.getProtocol());
            }
        }

        // Find all policies assigned to user, groups, equipment, or roles
        Set<Long> policyIds = new HashSet<>();

        // Policies assigned to user
        List<EquipmentPolicy> userPolicies = policyRepository.findAssignedToUser(user.getId());
        userPolicies.forEach(p -> policyIds.add(p.getId()));

        // Policies assigned to user's groups
        user.getGroups().forEach(group -> {
            List<EquipmentPolicy> groupPolicies = policyRepository.findAssignedToGroup(group.getId());
            groupPolicies.forEach(p -> policyIds.add(p.getId()));
        });

        // Policies assigned to user's roles
        user.getRoles().forEach(role -> {
            List<EquipmentPolicy> rolePolicies = policyRepository.findAssignedToRole(role.getId());
            rolePolicies.forEach(p -> policyIds.add(p.getId()));
        });

        // Only include equipment-assigned policies if user has at least one user/group/role assignment
        // This prevents unassigned users from accessing equipment via equipment-only policies
        if (!policyIds.isEmpty() && equipmentId != null) {
            List<EquipmentPolicy> equipmentPolicies = policyRepository.findAssignedToEquipment(equipmentId);
            equipmentPolicies.forEach(p -> policyIds.add(p.getId()));
        }

        request.setAssignedPolicyIds(policyIds);

        // Load policy details for all assigned policies
        if (!policyIds.isEmpty()) {
            List<EquipmentPolicy> policies = policyRepository.findAllById(policyIds);

            Set<String> allProtocols = new HashSet<>();
            Set<String> allDbms = new HashSet<>();
            Set<String> allBlacklistedCommands = new HashSet<>();
            Set<String> allWhitelistedCommands = new HashSet<>();
            Set<TimeSlot> allTimeSlots = new HashSet<>();
            Set<String> allAllowedIps = new HashSet<>();
            String ipFilteringType = null;

            for (EquipmentPolicy policy : policies) {
                if (!policy.isEnabled() || !"apply".equals(policy.getPolicyApplication())) {
                    continue;
                }

                // Load common settings (protocols, DBMS)
                PolicyCommonSettings commonSettings = policy.getCommonSettings();
                if (commonSettings != null) {
                    allProtocols.addAll(commonSettings.getAllowedProtocols().stream()
                            .map(PolicyAllowedProtocol::getProtocol)
                            .collect(Collectors.toSet()));

                    allDbms.addAll(commonSettings.getAllowedDbms().stream()
                            .map(PolicyAllowedDbms::getDbmsType)
                            .collect(Collectors.toSet()));
                }

                // Load command settings
                for (PolicyCommandSettings cmdSettings : policy.getCommandSettings()) {
                    for (CommandList cmdList : cmdSettings.getCommandLists()) {
                        Set<String> commands = cmdList.getItems().stream()
                                .map(CommandListItem::getCommandText)
                                .collect(Collectors.toSet());

                        if ("blacklist".equals(cmdList.getListType())) {
                            allBlacklistedCommands.addAll(commands);
                        } else if ("whitelist".equals(cmdList.getListType())) {
                            allWhitelistedCommands.addAll(commands);
                        }
                    }
                }

                // Load time slots
                PolicyAllowedTime allowedTime = policy.getAllowedTime();
                if (allowedTime != null) {
                    allTimeSlots.addAll(allowedTime.getTimeSlots().stream()
                            .map(ts -> new TimeSlot(ts.getDayOfWeek(), ts.getHourStart(), ts.getHourEnd()))
                            .collect(Collectors.toSet()));
                }

                // Load login control (IP filtering)
                PolicyLoginControl loginControl = policy.getLoginControl();
                if (loginControl != null) {
                    if (ipFilteringType == null) {
                        ipFilteringType = loginControl.getIpFilteringType();
                    }
                    allAllowedIps.addAll(loginControl.getAllowedIps().stream()
                            .map(PolicyAllowedIp::getIpAddress)
                            .collect(Collectors.toSet()));
                }
            }

            request.setAllowedProtocols(allProtocols);
            request.setAllowedDbms(allDbms);
            request.setBlacklistedCommands(allBlacklistedCommands);
            request.setWhitelistedCommands(allWhitelistedCommands);
            request.setAllowedTimeSlots(allTimeSlots);
            request.setAllowedIps(allAllowedIps);
            request.setIpFilteringType(ipFilteringType);
        }

        return request;
    }
}
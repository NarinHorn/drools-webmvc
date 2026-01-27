package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.model.entity.*;
import com.hunesion.drool_v2.model.EquipmentAccessRequest;
import com.hunesion.drool_v2.model.EquipmentAccessRequest.TimeSlot;
import com.hunesion.drool_v2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    private final CommandListRepository commandListRepository;
    private final PolicyConfigCache policyConfigCache;
    private final PolicyGroupRepository policyGroupRepository;

    @Autowired
    public PolicyFactLoader(
            EquipmentPolicyRepository policyRepository,
            UserRepository userRepository,
            EquipmentRepository equipmentRepository,
            CommandListRepository commandListRepository,
            PolicyConfigCache policyConfigCache,
            PolicyGroupRepository policyGroupRepository) {
        this.policyRepository = policyRepository;
        this.userRepository = userRepository;
        this.equipmentRepository = equipmentRepository;
        this.commandListRepository = commandListRepository;
        this.policyConfigCache = policyConfigCache;
        this.policyGroupRepository = policyGroupRepository;
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
                request.setDbmsType(equipment.getProtocol()); // Set DBMS type from protocol if applicable
                
                // Populate attributes map for custom conditions
                request.setAttribute("deviceType", equipment.getDeviceType());
                request.setAttribute("deviceName", equipment.getDeviceName());
                request.setAttribute("hostName", equipment.getHostName());
                request.setAttribute("ipAddress", equipment.getIpAddress());
                request.setAttribute("protocol", equipment.getProtocol());
                request.setAttribute("port", equipment.getPort());
            }
        }

        // Find all policies and policy groups assigned to user, groups, equipment, or roles
        Set<Long> policyIds = new HashSet<>();

        // Policies assigned to user
        List<EquipmentPolicy> userPolicies = policyRepository.findAssignedToUser(user.getId());
        userPolicies.forEach(p -> policyIds.add(p.getId()));
        System.out.println("Policies assigned to user: " + userPolicies);

        // Policies assigned to user's groups
        user.getGroups().forEach(group -> {
            List<EquipmentPolicy> groupPolicies = policyRepository.findAssignedToGroup(group.getId());
            groupPolicies.forEach(p -> policyIds.add(p.getId()));
            System.out.println("Policies assigned to user's groups: " + groupPolicies);
        });

        // Policies assigned to user's roles
        user.getRoles().forEach(role -> {
            List<EquipmentPolicy> rolePolicies = policyRepository.findAssignedToRole(role.getId());
            System.out.println("Policies assigned to user's roles: " + rolePolicies);
            rolePolicies.forEach(p -> policyIds.add(p.getId()));
        });

        // Policies from PolicyGroups assigned to user
        List<PolicyGroup> userPolicyGroups = policyGroupRepository.findAssignedToUser(user.getId());
        userPolicyGroups.forEach(pg -> {
            pg.getPolicyMembers().forEach(member -> policyIds.add(member.getPolicy().getId()));
        });
        System.out.println("Policies from PolicyGroups assigned to user: " + userPolicyGroups);

        // Policies from PolicyGroups assigned to user's groups
        user.getGroups().forEach(group -> {
            List<PolicyGroup> groupPolicyGroups = policyGroupRepository.findAssignedToUserGroup(group.getId());
            System.out.println("Policies from PolicyGroups assigned to user's groups: " + groupPolicyGroups);
            groupPolicyGroups.forEach(pg -> {
                pg.getPolicyMembers().forEach(member -> policyIds.add(member.getPolicy().getId()));
            });
        });

        // Policies from PolicyGroups assigned to user's roles
        user.getRoles().forEach(role -> {
            List<PolicyGroup> rolePolicyGroups = policyGroupRepository.findAssignedToRole(role.getId());
            System.out.println("Policies from PolicyGroups assigned to user's roles: " + rolePolicyGroups);
            rolePolicyGroups.forEach(pg -> {
                pg.getPolicyMembers().forEach(member -> policyIds.add(member.getPolicy().getId()));
            });
        });

        // Only include equipment-assigned policies if user has at least one user/group/role assignment
        // This prevents unassigned users from accessing equipment via equipment-only policies
        if (!policyIds.isEmpty() && equipmentId != null) {
            List<EquipmentPolicy> equipmentPolicies = policyRepository.findAssignedToEquipment(equipmentId);
            System.out.println("policies assigned to equipment: " + equipmentPolicies);
            equipmentPolicies.forEach(p -> policyIds.add(p.getId()));
        }

        request.setAssignedPolicyIds(policyIds);

        // Debug logging for policy resolution
        System.out.println("\n=== Policy Resolution Debug ===");
        System.out.println("User: " + username + " (ID: " + user.getId() + ")");
        System.out.println("User Roles: " + user.getRoleNames());
        System.out.println("User Groups: " + user.getGroupNames());
        System.out.println("Target Equipment ID: " + equipmentId);
        System.out.println("All Assigned Policy IDs: " + policyIds);

        // Load policy details for all assigned policies
        if (!policyIds.isEmpty()) {
            List<EquipmentPolicy> policies = policyRepository.findAllById(policyIds);

            Set<String> allProtocols = new HashSet<>();
            Set<String> allDbms = new HashSet<>();
            Set<String> allBlacklistedCommands = new HashSet<>();
            Set<String> allWhitelistedCommands = new HashSet<>();
            Set<TimeSlot> allTimeSlots = new HashSet<>();
            Set<String> allAllowedIps = new HashSet<>();
            final String[] ipFilteringType = {null}; // Use array to allow modification in lambda

            for (EquipmentPolicy policy : policies) {
                if (!policy.isEnabled() || !"apply".equals(policy.getPolicyApplication())) {
                    continue;
                }

                String policyConfigJson = policy.getPolicyConfig();
                if (policyConfigJson == null || policyConfigJson.isEmpty()) {
                    continue;
                }

                // Use cached JSONB config parsing
                Map<String, Object> config = policyConfigCache.getParsedConfig(
                    policy.getId(),
                    policyConfigJson
                );

                // Get policy type and aggregate based on type
                String typeCode = policy.getPolicyType().getTypeCode();

                switch (typeCode) {
                    case "commonSettings":
                        aggregateCommonSettings(config, allProtocols, allDbms);
                        break;
                    case "allowedTime":
                        aggregateAllowedTime(config, allTimeSlots);
                        break;
                    case "loginControl":
                        aggregateLoginControl(config, allAllowedIps, ipFilteringType);
                        break;
                    case "commandSettings":
                        aggregateCommandSettings(config, allBlacklistedCommands, allWhitelistedCommands);
                        break;
                }
            }

            request.setAllowedProtocols(allProtocols);
            request.setAllowedDbms(allDbms);
            request.setBlacklistedCommands(allBlacklistedCommands);
            request.setWhitelistedCommands(allWhitelistedCommands);
            request.setAllowedTimeSlots(allTimeSlots);
            request.setAllowedIps(allAllowedIps);
            request.setIpFilteringType(ipFilteringType[0]);

            // Continue debug logging
            System.out.println("Loaded Policies: " + policies.stream()
                    .map(p -> p.getPolicyName() + "(ID:" + p.getId() + ", enabled:" + p.isEnabled() + ")")
                    .collect(Collectors.joining(", ")));
            System.out.println("Allowed Protocols: " + allProtocols);
            System.out.println("Allowed DBMS: " + allDbms);
            System.out.println("Allowed TimeSlots: " + allTimeSlots.stream()
                .sorted((a, b) -> a.getDayOfWeek().compareTo(b.getDayOfWeek()))
                .map(ts -> "Day" + ts.getDayOfWeek() + "(" + ts.getHourStart() + "-" + ts.getHourEnd() + "h)")
                .collect(Collectors.joining(", ")));
            System.out.println("Current Request Time: Day" + request.getCurrentDayOfWeek() + " Hour" + request.getCurrentHour());
            System.out.println("isWithinAllowedTime: " + request.isWithinAllowedTime());
            System.out.println("================================");
        }

        return request;
    }

    /**
     * Aggregate commonSettings from policy config
     */
    private void aggregateCommonSettings(Map<String, Object> config, 
                                         Set<String> allProtocols, 
                                         Set<String> allDbms) {
        @SuppressWarnings("unchecked")
        Map<String, Object> commonSettings = (Map<String, Object>) config.get("commonSettings");
        if (commonSettings != null) {
            @SuppressWarnings("unchecked")
            List<String> protocols = (List<String>) commonSettings.get("allowedProtocols");
            if (protocols != null) {
                allProtocols.addAll(protocols);
            }

            @SuppressWarnings("unchecked")
            List<String> dbms = (List<String>) commonSettings.get("allowedDbms");
            if (dbms != null) {
                allDbms.addAll(dbms);
            }
        }
    }

    /**
     * Aggregate allowedTime from policy config
     */
    private void aggregateAllowedTime(Map<String, Object> config, Set<TimeSlot> allTimeSlots) {
        @SuppressWarnings("unchecked")
        Map<String, Object> allowedTime = (Map<String, Object>) config.get("allowedTime");
        if (allowedTime != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> timeSlots = (List<Map<String, Object>>) allowedTime.get("timeSlots");
            if (timeSlots != null) {
                timeSlots.forEach(ts -> {
                    Object dayOfWeekObj = ts.get("dayOfWeek");
                    Object hourStartObj = ts.get("hourStart");
                    Object hourEndObj = ts.get("hourEnd");
                    if (dayOfWeekObj != null && hourStartObj != null && hourEndObj != null) {
                        // Convert dayOfWeek to Integer (1=Monday, 7=Sunday)
                        Integer dayOfWeek;
                        if (dayOfWeekObj instanceof Integer) {
                            dayOfWeek = (Integer) dayOfWeekObj;
                        } else if (dayOfWeekObj instanceof Number) {
                            dayOfWeek = ((Number) dayOfWeekObj).intValue();
                        } else {
                            // Handle string day names (MONDAY, TUESDAY, etc.)
                            String dayStr = dayOfWeekObj.toString().toUpperCase();
                            dayOfWeek = convertDayNameToInteger(dayStr);
                        }

                        Integer hourStart = hourStartObj instanceof Integer ?
                            (Integer) hourStartObj : Integer.valueOf(hourStartObj.toString());
                        Integer hourEnd = hourEndObj instanceof Integer ?
                            (Integer) hourEndObj : Integer.valueOf(hourEndObj.toString());

                        if (dayOfWeek != null) {
                            allTimeSlots.add(new TimeSlot(dayOfWeek, hourStart, hourEnd));
                        }
                    }
                });
            }
        }
    }

    /**
     * Aggregate loginControl from policy config
     */
    private void aggregateLoginControl(Map<String, Object> config, 
                                       Set<String> allAllowedIps, 
                                       String[] ipFilteringType) {
        @SuppressWarnings("unchecked")
        Map<String, Object> loginControl = (Map<String, Object>) config.get("loginControl");
        if (loginControl != null) {
            String filteringType = (String) loginControl.get("ipFilteringType");
            if (ipFilteringType[0] == null && filteringType != null) {
                ipFilteringType[0] = filteringType;
            }

            @SuppressWarnings("unchecked")
            List<String> allowedIps = (List<String>) loginControl.get("allowedIps");
            if (allowedIps != null) {
                allAllowedIps.addAll(allowedIps);
            }
        }
    }

    /**
     * Aggregate commandSettings from policy config
     */
    private void aggregateCommandSettings(Map<String, Object> config,
                                          Set<String> allBlacklistedCommands,
                                          Set<String> allWhitelistedCommands) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> commandSettings = (List<Map<String, Object>>) config.get("commandSettings");
        if (commandSettings != null) {
            for (Map<String, Object> cmdSetting : commandSettings) {
                @SuppressWarnings("unchecked")
                List<Number> commandListIds = (List<Number>) cmdSetting.get("commandListIds");
                if (commandListIds != null) {
                    // Load CommandList entities (these stay normalized)
                    for (Number listId : commandListIds) {
                        CommandList cmdList = commandListRepository.findById(listId.longValue())
                                .orElse(null);
                        if (cmdList != null) {
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
                }
            }
        }
    }

    /**
     * Convert day name string to integer (1=Monday, 7=Sunday)
     */
    private Integer convertDayNameToInteger(String dayName) {
        if (dayName == null) {
            return null;
        }
        return switch (dayName.toUpperCase()) {
            case "MONDAY", "MON" -> 1;
            case "TUESDAY", "TUE" -> 2;
            case "WEDNESDAY", "WED" -> 3;
            case "THURSDAY", "THU" -> 4;
            case "FRIDAY", "FRI" -> 5;
            case "SATURDAY", "SAT" -> 6;
            case "SUNDAY", "SUN" -> 7;
            default -> {
                // Try to parse as integer
                try {
                    yield Integer.valueOf(dayName);
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }
}
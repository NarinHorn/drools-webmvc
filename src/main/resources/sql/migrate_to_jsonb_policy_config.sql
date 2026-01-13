-- Migration script: Convert EquipmentPolicy from normalized tables to JSONB policy_config
-- This script adds the new JSONB column and creates indexes
-- Note: Data migration from normalized tables to JSONB should be done separately

-- Step 1: Add new columns to equipment_policies table
ALTER TABLE equipment_policies 
ADD COLUMN IF NOT EXISTS policy_config JSONB,
ADD COLUMN IF NOT EXISTS generated_rule_drl TEXT,
ADD COLUMN IF NOT EXISTS version_no INTEGER DEFAULT 1,
ADD COLUMN IF NOT EXISTS created_by UUID,
ADD COLUMN IF NOT EXISTS last_updated_by UUID;

-- Step 2: Create index for JSONB queries (GIN index for efficient JSON queries)
CREATE INDEX IF NOT EXISTS idx_equipment_policies_config 
    ON equipment_policies USING GIN (policy_config);

-- Step 3: Create index for generated DRL (if needed for queries)
CREATE INDEX IF NOT EXISTS idx_equipment_policies_drl 
    ON equipment_policies(generated_rule_drl) 
    WHERE generated_rule_drl IS NOT NULL;

-- Step 4: Example migration query to convert existing normalized data to JSONB
-- NOTE: This is a template - you should test this on a backup first!
-- Uncomment and run this after verifying the structure matches your data

/*
UPDATE equipment_policies ep
SET policy_config = (
    SELECT jsonb_build_object(
        'commonSettings', COALESCE(
            (SELECT jsonb_build_object(
                'servicePort', pcs.service_port,
                'idleTimeMinutes', pcs.idle_time_minutes,
                'timeoutMinutes', pcs.timeout_minutes,
                'blockingPolicyType', pcs.blocking_policy_type,
                'sessionBlockingCount', pcs.session_blocking_count,
                'maxTelnetSessions', pcs.max_telnet_sessions,
                'telnetBorderless', pcs.telnet_borderless,
                'maxSshSessions', pcs.max_ssh_sessions,
                'sshBorderless', pcs.ssh_borderless,
                'maxRdpSessions', pcs.max_rdp_sessions,
                'rdpBorderless', pcs.rdp_borderless,
                'allowedProtocols', COALESCE(
                    (SELECT jsonb_agg(protocol) 
                     FROM policy_allowed_protocol 
                     WHERE policy_id = pcs.id),
                    '[]'::jsonb
                ),
                'allowedDbms', COALESCE(
                    (SELECT jsonb_agg(dbms_type) 
                     FROM policy_allowed_dbms 
                     WHERE policy_id = pcs.id),
                    '[]'::jsonb
                )
            )
            FROM policy_common_settings pcs
            WHERE pcs.policy_id = ep.id),
            '{}'::jsonb
        ),
        'allowedTime', COALESCE(
            (SELECT jsonb_build_object(
                'startDate', pat.start_date,
                'endDate', pat.end_date,
                'borderless', pat.borderless,
                'timeZone', pat.time_zone,
                'timeSlots', COALESCE(
                    (SELECT jsonb_agg(jsonb_build_object(
                        'dayOfWeek', day_of_week,
                        'hourStart', hour_start,
                        'hourEnd', hour_end
                    ))
                    FROM policy_time_slots
                    WHERE policy_id = pat.id),
                    '[]'::jsonb
                )
            )
            FROM policy_allowed_time pat
            WHERE pat.policy_id = ep.id),
            '{}'::jsonb
        ),
        'loginControl', COALESCE(
            (SELECT jsonb_build_object(
                'ipFilteringType', plc.ip_filtering_type,
                'accountLockEnabled', plc.account_lock_enabled,
                'maxFailureAttempts', plc.max_failure_attempts,
                'lockoutDurationMinutes', plc.lockout_duration_minutes,
                'twoFactorType', plc.two_factor_type,
                'allowedIps', COALESCE(
                    (SELECT jsonb_agg(ip_address)
                     FROM policy_allowed_ips
                     WHERE policy_id = plc.id),
                    '[]'::jsonb
                )
            )
            FROM policy_login_control plc
            WHERE plc.policy_id = ep.id),
            '{}'::jsonb
        ),
        'commandSettings', COALESCE(
            (SELECT jsonb_agg(jsonb_build_object(
                'protocolType', pcmd.protocol_type,
                'controlMethod', pcmd.control_method,
                'controlTarget', pcmd.control_target,
                'commandListIds', (
                    SELECT jsonb_agg(command_list_id::bigint)
                    FROM policy_command_lists
                    WHERE policy_id = pcmd.id
                )
            ))
            FROM policy_command_settings pcmd
            WHERE pcmd.policy_id = ep.id),
            '[]'::jsonb
        )
    )
)
WHERE EXISTS (
    SELECT 1 FROM policy_common_settings WHERE policy_id = ep.id
) OR EXISTS (
    SELECT 1 FROM policy_allowed_time WHERE policy_id = ep.id
) OR EXISTS (
    SELECT 1 FROM policy_login_control WHERE policy_id = ep.id
) OR EXISTS (
    SELECT 1 FROM policy_command_settings WHERE policy_id = ep.id
);
*/

-- Step 5: After verifying migration is complete and working correctly,
-- you can optionally drop the normalized tables (DO THIS CAREFULLY!):
-- 
-- DROP TABLE IF EXISTS policy_common_settings CASCADE;
-- DROP TABLE IF EXISTS policy_allowed_time CASCADE;
-- DROP TABLE IF EXISTS policy_login_control CASCADE;
-- DROP TABLE IF EXISTS policy_command_settings CASCADE;
-- DROP TABLE IF EXISTS policy_allowed_protocol CASCADE;
-- DROP TABLE IF EXISTS policy_allowed_dbms CASCADE;
-- DROP TABLE IF EXISTS policy_time_slots CASCADE;
-- DROP TABLE IF EXISTS policy_allowed_ips CASCADE;
-- DROP TABLE IF EXISTS policy_command_lists CASCADE;

-- Step 6: Verify migration
-- Run this query to check if policies have been migrated:
-- SELECT id, policy_name, 
--        CASE 
--            WHEN policy_config IS NULL THEN 'Not migrated'
--            WHEN policy_config = '{}'::jsonb THEN 'Empty config'
--            ELSE 'Migrated'
--        END as migration_status,
--        jsonb_object_keys(policy_config) as config_keys
-- FROM equipment_policies
-- ORDER BY id;

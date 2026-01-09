-- ============================================
-- Equipment Table Creation Script
-- For i-oneNGS System
-- ============================================
-- 
-- This script creates the equipment table for managing
-- devices/equipment (Linux servers, databases, etc.)
-- ============================================

-- Create equipment table
CREATE TABLE IF NOT EXISTS equipment (
    id BIGSERIAL PRIMARY KEY,
    device_name VARCHAR(200) NOT NULL,
    host_name VARCHAR(255),
    ip_address VARCHAR(45),
    protocol VARCHAR(50),
    port INTEGER,
    username VARCHAR(255),
    password VARCHAR(500),
    device_type VARCHAR(50),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on device_name for faster lookups
CREATE INDEX IF NOT EXISTS idx_equipment_device_name ON equipment(device_name) WHERE is_deleted = FALSE;

-- Create index on device_type
CREATE INDEX IF NOT EXISTS idx_equipment_device_type ON equipment(device_type) WHERE is_deleted = FALSE;

-- Create unique constraint on device_name for non-deleted records
-- Note: This requires a partial unique index
CREATE UNIQUE INDEX IF NOT EXISTS idx_equipment_device_name_unique 
ON equipment(device_name) 
WHERE is_deleted = FALSE;

-- Add comment to table
COMMENT ON TABLE equipment IS 'Stores equipment/device information (Linux servers, databases, etc.)';
COMMENT ON COLUMN equipment.device_name IS 'Unique name for the equipment/device';
COMMENT ON COLUMN equipment.host_name IS 'Hostname of the device';
COMMENT ON COLUMN equipment.ip_address IS 'IP address of the device (supports IPv4 and IPv6)';
COMMENT ON COLUMN equipment.protocol IS 'Connection protocol (e.g., ssh, rdp, http, https, vnc)';
COMMENT ON COLUMN equipment.port IS 'Connection port number (e.g., 22 for SSH, 3389 for RDP)';
COMMENT ON COLUMN equipment.username IS 'Username for connection authentication';
COMMENT ON COLUMN equipment.password IS 'Password for connection authentication (stored securely)';
COMMENT ON COLUMN equipment.device_type IS 'Type of device (e.g., LINUX_SERVER, DATABASE, WINDOWS_SERVER)';
COMMENT ON COLUMN equipment.is_deleted IS 'Soft delete flag - true if equipment is deleted';

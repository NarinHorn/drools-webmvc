-- ============================================
-- Add Connection Fields to Equipment Table
-- Migration Script for Existing Tables
-- ============================================
-- 
-- This script adds protocol, port, username, and password fields
-- to existing equipment table
-- ============================================

-- Add new columns to equipment table
ALTER TABLE equipment 
ADD COLUMN IF NOT EXISTS protocol VARCHAR(50),
ADD COLUMN IF NOT EXISTS port INTEGER,
ADD COLUMN IF NOT EXISTS username VARCHAR(255),
ADD COLUMN IF NOT EXISTS password VARCHAR(500);

-- Add comments
COMMENT ON COLUMN equipment.protocol IS 'Connection protocol (e.g., ssh, rdp, http, https, vnc)';
COMMENT ON COLUMN equipment.port IS 'Connection port number (e.g., 22 for SSH, 3389 for RDP)';
COMMENT ON COLUMN equipment.username IS 'Username for connection authentication';
COMMENT ON COLUMN equipment.password IS 'Password for connection authentication (stored securely)';

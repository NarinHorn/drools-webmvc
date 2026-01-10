-- Create user_equipment join table for many-to-many relationship
-- This table links users to equipment/devices they have access to
-- One user can have access to many equipment, and one equipment can be assigned to many users

CREATE TABLE IF NOT EXISTS user_equipment (
    user_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, equipment_id),
    CONSTRAINT fk_user_equipment_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_user_equipment_equipment 
        FOREIGN KEY (equipment_id) 
        REFERENCES equipment(id) 
        ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_user_equipment_user_id ON user_equipment(user_id);
CREATE INDEX IF NOT EXISTS idx_user_equipment_equipment_id ON user_equipment(equipment_id);

-- Add comment to table
COMMENT ON TABLE user_equipment IS 'Join table for many-to-many relationship between users and equipment. Represents which users have access to which equipment/devices.';

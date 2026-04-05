-- Add startedAt timestamp to track when job execution begins
ALTER TABLE job ADD COLUMN IF NOT EXISTS started_at TIMESTAMP;

-- Add report column to job table to store simulation warnings and errors
ALTER TABLE job ADD COLUMN IF NOT EXISTS report jsonb;

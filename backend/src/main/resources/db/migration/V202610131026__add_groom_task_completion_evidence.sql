-- What the Groom records when ticking a daily task: an optional note and an optional photo.
-- "notes" already holds the task's instructions, so the completion note gets its own column.
ALTER TABLE groom_daily_tasks
    ADD COLUMN IF NOT EXISTS completion_note      TEXT,
    ADD COLUMN IF NOT EXISTS completion_image_url VARCHAR(500);

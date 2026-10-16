-- A Groom completes several routine tasks at once (one duty, one time, several horses) and records one note
-- and up to five photos for that action. The evidence therefore belongs to the completion, not to each task.

CREATE TABLE IF NOT EXISTS groom_task_completions (
    id           BIGSERIAL PRIMARY KEY,
    groom_id     BIGINT    NOT NULL REFERENCES users (id),
    note         TEXT,
    completed_at TIMESTAMP NOT NULL DEFAULT now(),
    -- Temporary link used only to move the evidence stored on tasks so far; dropped below.
    source_task_id BIGINT
);

CREATE TABLE IF NOT EXISTS groom_task_completion_photos (
    completion_id BIGINT       NOT NULL REFERENCES groom_task_completions (id) ON DELETE CASCADE,
    position      INT          NOT NULL,
    image_key     VARCHAR(500) NOT NULL,
    PRIMARY KEY (completion_id, position)
);

ALTER TABLE groom_daily_tasks
    ADD COLUMN IF NOT EXISTS completion_id BIGINT REFERENCES groom_task_completions (id);

CREATE INDEX IF NOT EXISTS idx_groom_daily_tasks_completion ON groom_daily_tasks (completion_id);
CREATE INDEX IF NOT EXISTS idx_groom_task_completions_groom ON groom_task_completions (groom_id);

-- Evidence recorded on single tasks before this change: one completion per task.
INSERT INTO groom_task_completions (groom_id, note, completed_at, source_task_id)
SELECT task.groom_id, task.completion_note, COALESCE(task.completed_at, now()), task.id
FROM groom_daily_tasks task
WHERE task.completion_note IS NOT NULL OR task.completion_image_url IS NOT NULL;

INSERT INTO groom_task_completion_photos (completion_id, position, image_key)
SELECT completion.id, 0, task.completion_image_url
FROM groom_task_completions completion
JOIN groom_daily_tasks task ON task.id = completion.source_task_id
WHERE task.completion_image_url IS NOT NULL;

UPDATE groom_daily_tasks task
SET completion_id = completion.id
FROM groom_task_completions completion
WHERE completion.source_task_id = task.id;

ALTER TABLE groom_task_completions DROP COLUMN source_task_id;
ALTER TABLE groom_daily_tasks DROP COLUMN IF EXISTS completion_note;
ALTER TABLE groom_daily_tasks DROP COLUMN IF EXISTS completion_image_url;

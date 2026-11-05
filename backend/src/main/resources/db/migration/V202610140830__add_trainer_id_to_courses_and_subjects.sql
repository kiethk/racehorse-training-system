-- Migration: Add trainer_id to courses and subjects to make them trainer-specific
-- File: V202610140830__add_trainer_id_to_courses_and_subjects.sql

-- 1. Thêm cột trainer_id vào bảng courses
ALTER TABLE courses ADD COLUMN IF NOT EXISTS trainer_id BIGINT REFERENCES users(id);

-- Cập nhật dữ liệu cũ cho courses: ưu tiên created_by_id, nếu null thì gán cho HEAD_TRAINER đầu tiên
UPDATE courses
SET trainer_id = created_by_id
WHERE trainer_id IS NULL AND created_by_id IS NOT NULL;

UPDATE courses
SET trainer_id = (
    SELECT u.id
    FROM users u
    JOIN roles r ON u.role_id = r.id
    WHERE r.name = 'HEAD_TRAINER'
    ORDER BY u.id ASC
    LIMIT 1
)
WHERE trainer_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_courses_trainer_id ON courses(trainer_id);

-- 2. Thêm cột trainer_id vào bảng subjects
ALTER TABLE subjects ADD COLUMN IF NOT EXISTS trainer_id BIGINT REFERENCES users(id);

-- Cập nhật dữ liệu cũ cho subjects: gán cho HEAD_TRAINER đầu tiên trong hệ thống
UPDATE subjects
SET trainer_id = (
    SELECT u.id
    FROM users u
    JOIN roles r ON u.role_id = r.id
    WHERE r.name = 'HEAD_TRAINER'
    ORDER BY u.id ASC
    LIMIT 1
)
WHERE trainer_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_subjects_trainer_id ON subjects(trainer_id);

-- V202611041200 — Track 5 (AD-022 / AD-025): xoa bo CANCELLED, chuan hoa OVERDUE trong vong doi care_schedule.
-- 1) Don du lieu CANCELLED con ton (chi la san pham seed: khong co health_record, khong co incident, khong co con — da do bang pg_constraint + count).
--    Xoa theo AD-025 rule 6: lich chua dien ra bi supersed thi xoa cung, dau vet nam o ca thay the / notification.
DELETE FROM care_schedule WHERE status = 'CANCELLED';

-- 2) CHECK constraint moc chan trang thai (AD-022: dung 6 gia tri). Day la constraint dau tien
--    tren care_schedule.status — truoc day chua ton tai du ROADMAP/HANDOFF mo ta nhu co san.
ALTER TABLE care_schedule DROP CONSTRAINT IF EXISTS chk_care_schedules_status;
ALTER TABLE care_schedule ADD CONSTRAINT chk_care_schedules_status
    CHECK (status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS', 'PAUSED', 'OVERDUE', 'COMPLETED'));

-- 3) BON index bo phan (partial) cua care_schedule loc theo status. Chung KHONG phai la
--    index thong thuong: hai cai la UNIQUE BUG DUP, hai cai la guard chong trung gio.
--    Neu khong them OVERDUE thi khi ca URGENT/Preventive chuyen OVERDUE no roi khoi
--    dieu kiens unique -> ngua nhan ca cap cu thu hai, hoac moc tiem bi tao dup.
--    (Thuong dan: ROADMAP/HANDOFF khong nep nay duoc.)
DROP INDEX IF EXISTS uq_care_schedule_one_active_urgent_per_horse;
CREATE UNIQUE INDEX uq_care_schedule_one_active_urgent_per_horse
    ON care_schedule(horse_id)
    WHERE care_type = 'URGENT' AND status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS', 'OVERDUE');

DROP INDEX IF EXISTS uq_care_schedule_one_active_preventive_per_horse;
CREATE UNIQUE INDEX uq_care_schedule_one_active_preventive_per_horse
    ON care_schedule(horse_id)
    WHERE purpose = 'PREVENTIVE' AND status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS', 'OVERDUE');

DROP INDEX IF EXISTS idx_care_schedule_vet_slot;
CREATE INDEX idx_care_schedule_vet_slot
    ON care_schedule(veterinarian_id, scheduled_at)
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS', 'OVERDUE');

DROP INDEX IF EXISTS idx_care_schedule_horse_slot;
CREATE INDEX idx_care_schedule_horse_slot
    ON care_schedule(horse_id, scheduled_at)
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS', 'OVERDUE');

-- 4) cancel_reason giu cot cho du lieu lich su (ddl-auto=validate), nhung khong con duoc ghi moi.
COMMENT ON COLUMN care_schedule.cancel_reason IS
    'LEGACY (AD-022): no longer written; retained for historical rows only';

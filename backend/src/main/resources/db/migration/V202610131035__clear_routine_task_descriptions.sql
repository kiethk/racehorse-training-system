-- Routine (SOP) tasks no longer carry a description: the task type and the time already say what to do.
-- This clears the descriptions generated so far, in both the English and the earlier Vietnamese wording.
-- Notes typed by hand on ad-hoc tasks are different text and are left alone.
UPDATE groom_daily_tasks
SET notes = NULL
WHERE notes IN (
    'Morning feed, following the approved ration',
    'First muck-out: remove manure and replace bedding before training',
    'Full wash, grooming and hoof oil after training (10:00 - 11:30)',
    'Midday feed; check and refill the water trough',
    'Evening feed, top up hay, and second muck-out',
    'Cho ăn sáng theo khẩu phần dinh dưỡng đã duyệt',
    'Dọn phân, thay rơm lót chuồng đợt 1 trước giờ tập',
    'Tắm rửa toàn thân, chải lông, bôi dầu dưỡng móng sau buổi tập (10:00 - 11:30)',
    'Cho ăn trưa và kiểm tra bổ sung máng nước sạch',
    'Cho ăn chiều, bổ sung cỏ khô và dọn vệ sinh chuồng đợt 2'
);

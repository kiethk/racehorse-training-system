-- 1. Cập nhật quyết định y tế cho toàn bộ ngựa trong khu chuồng cách ly (QUARANTINE) thành BLOCKED
UPDATE horses
SET training_decision = 'BLOCKED',
    training_decision_reason = 'Cách ly kiểm dịch y tế bắt buộc tại khu chuồng cách ly'
WHERE current_stall_id IN (
    SELECT s.id FROM stable_stalls s
    JOIN areas a ON s.area_id = a.id
    WHERE a.type = 'QUARANTINE'
);

-- 2. Dữ liệu mẫu hồ sơ tri thức y khoa lâm sàng RAG (rag_knowledge_documents & rag_knowledge_embeddings)
INSERT INTO rag_knowledge_documents (id, title, category, source_url, allowed_roles, version, is_active)
VALUES 
(1, 'Phác đồ điều trị viêm gân gót và dây chằng treo (Superficial Digital Flexor Tendonitis - SDFT)', 'ORTHOPEDIC', 'https://rtms.internal/clinical/sdft-protocol', '{"VETERINARIAN", "HEAD_TRAINER"}', '1.0', true),
(2, 'Quy trình cấp cứu và điều trị chứng đau bụng cấp tính ở ngựa (Equine Colic)', 'GASTROINTESTINAL', 'https://rtms.internal/clinical/colic-emergency', '{"VETERINARIAN"}', '1.0', true),
(3, 'Phác đồ quản lý viêm khớp cổ chân và khớp bàn ngón (Equine Fetlock Arthritis)', 'ORTHOPEDIC', 'https://rtms.internal/clinical/fetlock-arthritis', '{"VETERINARIAN", "HEAD_TRAINER"}', '1.0', true),
(4, 'Xử trí xuất huyết phổi do gắng sức ở ngựa đua (Exercise-Induced Pulmonary Hemorrhage - EIPH)', 'RESPIRATORY', 'https://rtms.internal/clinical/eiph-protocol', '{"VETERINARIAN", "HEAD_TRAINER"}', '1.0', true),
(5, 'Điều trị áp xe móng và nứt móng thể nặng (Hoof Abscess & Quarter Crack)', 'HOOF_CARE', 'https://rtms.internal/clinical/hoof-abscess', '{"VETERINARIAN", "GROOM"}', '1.0', true)
ON CONFLICT (id) DO NOTHING;

-- Embeddings tương ứng (Vector 1536 chiều mô phỏng đã chuẩn hóa)
INSERT INTO rag_knowledge_embeddings (document_id, chunk_index, content, metadata, embedding)
VALUES 
(1, 0, 'Ca lâm sàng viêm gân gót sau bài tập tốc độ cao: Ngựa có triệu chứng sưng nóng, đau khi ấn vào 1/3 giữa gân gấp ngón nông (SDFT). Phác đồ can thiệp khẩn cấp: 1. Đình chỉ tập luyện hoàn toàn (BLOCKED 4-6 tuần). 2. Chườm lạnh 20 phút x 3 lần/ngày trong 48h đầu. 3. Băng cố định đàn hồi. 4. Dùng thuốc chống viêm NSAID (Phenylbutazone 2.2 mg/kg IV). 5. Siêu âm đánh giá sau 14 ngày để xem xét tiêm huyết tương giàu tiểu cầu (PRP).', '{"tags": ["viêm gân", "gân gót", "SDFT", "chấn thương chân"], "category": "ORTHOPEDIC"}', ('[' || array_to_string(array_fill(0.025::float4, ARRAY[1536]), ',') || ']')::vector),
(2, 0, 'Ca lâm sàng hội chứng Colic (đau bụng cấp): Triệu chứng quan sát thấy cào đất, quay đầu nhìn hông sườn, lăn lộn dữ dội, mạch đập nhanh (>60 bpm), mất nhu động ruột. Phác đồ điều trị: 1. Đặt ống thông dạ dày giải áp và kiểm tra trào ngược. 2. Bơm dầu khoáng 2-4 lít nhuận tràng. 3. Truyền dịch tĩnh mạch Ringer Lactate hồi sức. 4. Giảm đau bằng Flunixin meglumine (1.1 mg/kg IV). 5. Ngừng ăn chất thô, theo dõi sát trong khu chuồng chăm sóc đặc biệt.', '{"tags": ["đau bụng", "colic", "cào đất", "tiêu hóa"], "category": "GASTROINTESTINAL"}', ('[' || array_to_string(array_fill(0.025::float4, ARRAY[1536]), ',') || ']')::vector),
(3, 0, 'Ca lâm sàng thoái hóa và tràn dịch khớp cổ chân: Ngựa khập khiễng mức độ 2/5 sau ca tập trên mặt sân cứng. Phác đồ điều trị: Tiêm nội khớp Hyaluronic Acid kết hợp Triamcinolone acetonide dưới hướng dẫn siêu âm vô trùng. Cho nghỉ tập 7 ngày, sau đó phục hồi chức năng bằng bài tập đi bộ nhẹ trên sân cỏ 15 phút/ngày.', '{"tags": ["viêm khớp", "cổ chân", "khập khiễng", "tiêm khớp"], "category": "ORTHOPEDIC"}', ('[' || array_to_string(array_fill(0.025::float4, ARRAY[1536]), ',') || ']')::vector),
(4, 0, 'Ca lâm sàng EIPH (chảy máu phổi do gắng sức): Sau khi phi nước đại đạt tốc độ đỉnh, phát hiện máu chảy ở cửa mũi hai bên. Phác đồ can thiệp: Giảm tải cường độ bài tập, nội soi khí phế quản chẩn đoán mức độ xuất huyết. Sử dụng Furosemide (0.5 mg/kg IV) 4 giờ trước các buổi chạy thử và bổ sung vitamin C hỗ trợ thành mạch.', '{"tags": ["hô hấp", "chảy máu mũi", "EIPH", "xuất huyết phổi"], "category": "RESPIRATORY"}', ('[' || array_to_string(array_fill(0.025::float4, ARRAY[1536]), ',') || ']')::vector),
(5, 0, 'Ca lâm sàng áp xe móng chân trước: Ngựa nhấc chân không dám chịu lực, mạch ngón đập mạnh, gõ móng có phản xạ đau rõ. Phác đồ: Khoét lỗ thoát mủ đáy móng, ngâm dung dịch muối Epsom ấm và Povidone Iodine 15 phút. Băng móng vô trùng bằng Animalintex, tiêm huyết thanh kháng uốn ván (Tetanus antitoxin).', '{"tags": ["móng ngựa", "áp xe móng", "nhiễm trùng móng"], "category": "HOOF_CARE"}', ('[' || array_to_string(array_fill(0.025::float4, ARRAY[1536]), ',') || ']')::vector)
ON CONFLICT DO NOTHING;

-- 3. Dữ liệu mẫu báo cáo sự cố chuồng trại trong 48h qua (horse_incident_reports)
INSERT INTO horse_incident_reports (reported_by_id, horse_id, title, description, severity, status, reported_at)
SELECT 
    10, 
    50, 
    'Ngựa bước đi khập khiễng chân sau bên phải', 
    'Sau khi đưa ngựa ra khu tập dượt sáng nay, phát hiện ngựa nhấc chân sau bên phải, có biểu hiện đau khi tỳ lực. Khớp cổ chân hơi sưng ấm.', 
    'HIGH', 
    'REPORTED', 
    NOW() - INTERVAL '3 hours'
WHERE EXISTS (SELECT 1 FROM users WHERE id = 10) AND EXISTS (SELECT 1 FROM horses WHERE id = 50);

INSERT INTO horse_incident_reports (reported_by_id, horse_id, title, description, severity, status, reported_at)
SELECT 
    11, 
    51, 
    'Ngựa cào đất và bỏ dở máng thức ăn', 
    'Phát hiện ngựa liên tục cào móng xuống nền chuồng, quay đầu nhìn sườn và không ăn khẩu phần cám chiều. Bụng có tiếng réo bất thường.', 
    'HIGH', 
    'REPORTED', 
    NOW() - INTERVAL '14 hours'
WHERE EXISTS (SELECT 1 FROM users WHERE id = 11) AND EXISTS (SELECT 1 FROM horses WHERE id = 51);

INSERT INTO horse_incident_reports (reported_by_id, horse_id, title, description, severity, status, reported_at)
SELECT 
    10, 
    52, 
    'Trầy xước da nhẹ vùng đùi sau khi dắt chuồng', 
    'Ngựa bị trầy xước nhẹ lớp da bên ngoài do quẹt vào mép cửa chuồng trong lúc dẫn về. Không chảy máu nhiều, đã sát trùng sơ bộ bằng povidone.', 
    'LOW', 
    'REPORTED', 
    NOW() - INTERVAL '26 hours'
WHERE EXISTS (SELECT 1 FROM users WHERE id = 10) AND EXISTS (SELECT 1 FROM horses WHERE id = 52);

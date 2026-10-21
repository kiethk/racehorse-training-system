-- Seed baseline equine medical symptoms dictionary for RAG expander

INSERT INTO medical_symptom_mappings (raw_symptom, medical_term, category) VALUES
-- 1. Tiêu hóa & Đau bụng (Digestive & Colic)
('cào đất', 'đau bụng cấp tính ở ngựa (equine colic)', 'DIGESTIVE'),
('cao dat', 'đau bụng cấp tính ở ngựa (equine colic)', 'DIGESTIVE'),
('pawing', 'đau bụng cấp tính ở ngựa (equine colic)', 'DIGESTIVE'),
('nhìn bụng', 'đau bụng cấp tính (colic)', 'DIGESTIVE'),
('quay đầu nhìn sườn', 'đau bụng cấp tính (colic)', 'DIGESTIVE'),
('nhìn mạn sườn', 'căng tức vùng bụng nghi ngờ xoắn ruột', 'DIGESTIVE'),
('nằm lăn', 'đau bụng cấp dữ dội (severe colic)', 'DIGESTIVE'),
('lăn lộn', 'tắc ruột (impaction colic)', 'DIGESTIVE'),
('đá vào bụng', 'đau quặn bụng (abdominal pain)', 'DIGESTIVE'),
('bỏ ăn', 'suy nhược (anorexia)', 'DIGESTIVE'),
('chán ăn', 'loét dạ dày (equine gastric ulcer syndrome - EGUS)', 'DIGESTIVE'),
('không đi phân', 'tắc nghẽn kết tràng (colonic impaction)', 'DIGESTIVE'),
('táo bón', 'tắc nghẽn kết tràng (colonic impaction)', 'DIGESTIVE'),
('phân khô', 'mất nước đường tiêu hóa', 'DIGESTIVE'),
('tiêu chảy', 'viêm đại tràng (colitis)', 'DIGESTIVE'),

-- 2. Hệ vận động & Chi dưới (Locomotor & Musculoskeletal)
('tập tễnh', 'què/khập khiễng (equine lameness)', 'LOCOMOTOR'),
('khập khiễng', 'què/khập khiễng (equine lameness)', 'LOCOMOTOR'),
('đi nhắc chân', 'chấn thương cơ xương khớp', 'LOCOMOTOR'),
('nóng móng', 'viêm màng đáy móng (laminitis)', 'LOCOMOTOR'),
('móng nóng', 'áp xe móng (hoof abscess)', 'LOCOMOTOR'),
('đập mạch móng', 'viêm màng đáy móng cấp (acute laminitis)', 'LOCOMOTOR'),
('sưng chân', 'viêm gân gấp ngón (tendinitis)', 'LOCOMOTOR'),
('sưng gân', 'viêm gân sau (bowed tendon)', 'LOCOMOTOR'),
('sưng khớp', 'viêm khớp (arthritis / synovitis)', 'LOCOMOTOR'),
('cứng cơ', 'hội chứng tiêu cơ vân do gắng sức (tying-up)', 'LOCOMOTOR'),

-- 3. Hệ hô hấp (Respiratory)
('chảy nước mũi', 'viêm đường hô hấp trên', 'RESPIRATORY'),
('dịch mũi vàng', 'viêm xoang hoặc nhiễm khuẩn Streptococcus equi', 'RESPIRATORY'),
('ho', 'viêm phế quản hoặc hen suyễn ở ngựa (equine asthma)', 'RESPIRATORY'),
('ho khan', 'hen ngựa (recurrent airway obstruction - RAO)', 'RESPIRATORY'),
('thở dốc', 'suy hô hấp cấp', 'RESPIRATORY'),
('thở gấp', 'viêm phổi (pneumonia)', 'RESPIRATORY'),
('chảy máu mũi', 'xuất huyết phổi do gắng sức (EIPH)', 'RESPIRATORY'),

-- 4. Da & Nhiễm khuẩn / Dị ứng (Dermatology)
('rụng lông', 'viêm da dị ứng do côn trùng (sweet itch)', 'DERMATOLOGY'),
('ngứa da', 'nấm da (ringworm)', 'DERMATOLOGY'),
('vết loét', 'nhiễm trùng mô mềm (cellulitis)', 'DERMATOLOGY'),
('trầy xước', 'vết thương nhiễm khuẩn ngoài da', 'DERMATOLOGY'),

-- 5. Thần kinh & Toàn thân (Systemic & Neurological)
('sốt cao', 'sốt nhiễm trùng toàn thân hoặc nhiễm EHV', 'SYSTEMIC'),
('người nóng', 'sốt nhiễm trùng toàn thân', 'SYSTEMIC'),
('mất thăng bằng', 'bệnh lý thần kinh tủy sống (EPM)', 'SYSTEMIC'),
('đi lảo đảo', 'chứng mất điều hòa vận động (ataxia)', 'SYSTEMIC'),
('mất nước', 'mất nước nghiêm trọng (dehydration)', 'SYSTEMIC');

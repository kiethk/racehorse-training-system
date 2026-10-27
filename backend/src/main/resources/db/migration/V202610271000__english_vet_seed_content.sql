-- English-only content for Veterinarian seed data.
-- The original seeds (V202610210005, V202610200015) were written in Vietnamese.
-- This migration rewrites the already-seeded rows in place; it is idempotent —
-- every UPDATE matches the Vietnamese text, so re-running (or running on a fresh
-- database after those seeds) converges to the same English content.

-- 1. Quarantine hold reason shown on the Vet dashboard watchlist.
UPDATE horses
SET training_decision_reason = 'Mandatory medical quarantine isolation in the quarantine barn'
WHERE training_decision_reason = 'Cách ly kiểm dịch y tế bắt buộc tại khu chuồng cách ly';

-- 2. RAG knowledge documents (visible in the Vet AI assistant).
UPDATE rag_knowledge_documents SET title = 'Treatment protocol for superficial digital flexor tendonitis (SDFT) and suspensory ligament injuries'
WHERE title = 'Phác đồ điều trị viêm gân gót và dây chằng treo (Superficial Digital Flexor Tendonitis - SDFT)';
UPDATE rag_knowledge_documents SET title = 'Emergency management and treatment of equine colic'
WHERE title = 'Quy trình cấp cứu và điều trị chứng đau bụng cấp tính ở ngựa (Equine Colic)';
UPDATE rag_knowledge_documents SET title = 'Management protocol for fetlock arthritis and synovitis'
WHERE title = 'Phác đồ quản lý viêm khớp cổ chân và khớp bàn ngón (Equine Fetlock Arthritis)';
UPDATE rag_knowledge_documents SET title = 'Management of exercise-induced pulmonary hemorrhage (EIPH) in racehorses'
WHERE title = 'Xử trí xuất huyết phổi do gắng sức ở ngựa đua (Exercise-Induced Pulmonary Hemorrhage - EIPH)';
UPDATE rag_knowledge_documents SET title = 'Treatment of hoof abscess and severe quarter cracks'
WHERE title = 'Điều trị áp xe móng và nứt móng thể nặng (Hoof Abscess & Quarter Crack)';

-- 3. RAG knowledge chunks: clinical case content and tags.
UPDATE rag_knowledge_embeddings
SET content = 'Clinical case: superficial digital flexor tendonitis after high-speed exercise. The horse presents heat, swelling and pain on palpation of the mid-portion of the superficial digital flexor tendon (SDFT). Emergency intervention protocol: 1. Suspend all training completely (BLOCKED for 4-6 weeks). 2. Cold therapy 20 minutes, 3 times daily for the first 48 hours. 3. Supportive elastic bandaging. 4. NSAID anti-inflammatory therapy (Phenylbutazone 2.2 mg/kg IV). 5. Ultrasound re-evaluation after 14 days to consider platelet-rich plasma (PRP) therapy.',
    metadata = '{"tags": ["tendonitis", "SDFT", "suspensory injury", "leg injury"], "category": "ORTHOPEDIC"}'
WHERE document_id = 1 AND chunk_index = 0;

UPDATE rag_knowledge_embeddings
SET content = 'Clinical case: equine colic syndrome (acute abdominal pain). Observed signs include pawing the ground, turning the head toward the flank, violent rolling, elevated heart rate (>60 bpm) and loss of gut motility. Treatment protocol: 1. Pass a nasogastric tube to decompress the stomach and check for reflux. 2. Administer 2-4 liters of mineral oil as a laxative. 3. Intravenous fluid therapy with Ringer Lactate for resuscitation. 4. Analgesia with Flunixin meglumine (1.1 mg/kg IV). 5. Withhold roughage and monitor closely in the intensive care stall.',
    metadata = '{"tags": ["colic", "abdominal pain", "pawing", "digestive"], "category": "GASTROINTESTINAL"}'
WHERE document_id = 2 AND chunk_index = 0;

UPDATE rag_knowledge_embeddings
SET content = 'Clinical case: fetlock joint degeneration and effusion. The horse shows 2/5 lameness after training on hard ground. Treatment protocol: intra-articular Hyaluronic Acid combined with Triamcinolone acetonide injection under sterile ultrasound guidance. Rest from training for 7 days, then rehabilitation with light walking exercise on grass, 15 minutes daily.',
    metadata = '{"tags": ["arthritis", "fetlock", "lameness", "joint injection"], "category": "ORTHOPEDIC"}'
WHERE document_id = 3 AND chunk_index = 0;

UPDATE rag_knowledge_embeddings
SET content = 'Clinical case: exercise-induced pulmonary hemorrhage (EIPH). Bilateral epistaxis (bleeding from both nostrils) observed after the horse reached peak gallop speed. Intervention protocol: reduce training intensity and confirm the hemorrhage grade by diagnostic endoscopy. Use Furosemide (0.5 mg/kg IV) 4 hours before trial runs and supplement vitamin C to support capillary health.',
    metadata = '{"tags": ["respiratory", "nosebleed", "EIPH", "pulmonary hemorrhage"], "category": "RESPIRATORY"}'
WHERE document_id = 4 AND chunk_index = 0;

UPDATE rag_knowledge_embeddings
SET content = 'Clinical case: front hoof abscess. The horse will not bear weight on the affected foot, the digital pulse is strong, and hoof percussion shows a clear pain response. Protocol: pare and drain the abscess at the sole, soak the foot in warm Epsom salt and Povidone Iodine solution for 15 minutes, apply a sterile Animalintex bandage, and administer tetanus antitoxin.',
    metadata = '{"tags": ["hoof", "hoof abscess", "hoof infection"], "category": "HOOF_CARE"}'
WHERE document_id = 5 AND chunk_index = 0;

-- 4. Seeded stable incident reports (Groom → Vet urgent reports).
UPDATE horse_incident_reports
SET title = 'Horse walking lame on the right hind leg',
    description = 'While bringing the horse out to the training area this morning, the right hind leg was noticed lifted and painful on weight bearing. The fetlock joint is slightly swollen and warm.'
WHERE title = 'Ngựa bước đi khập khiễng chân sau bên phải';

UPDATE horse_incident_reports
SET title = 'Horse pawing the ground and leaving feed uneaten',
    description = 'The horse was repeatedly pawing the stall floor, turning its head toward the flank, and refused its evening grain. Abnormal gut sounds were heard in the abdomen.'
WHERE title = 'Ngựa cào đất và bỏ dở máng thức ăn';

UPDATE horse_incident_reports
SET title = 'Mild thigh skin abrasion after leading back to the barn',
    description = 'The horse lightly scraped the outer skin of the hind thigh against the stable door edge while being led back. No heavy bleeding; the area was disinfected with povidone iodine.'
WHERE title = 'Trầy xước da nhẹ vùng đùi sau khi dắt chuồng';

-- 5. Medical symptom dictionary (RAG expander). Vietnamese raw_symptom keys are
-- kept: they match symptom text typed in Vietnamese. The displayed medical term
-- becomes English.
UPDATE medical_symptom_mappings m
SET medical_term = v.en
FROM (VALUES
  ('đau bụng cấp tính ở ngựa (equine colic)', 'Acute equine colic'),
  ('đau bụng cấp tính (colic)', 'Acute colic (abdominal pain)'),
  ('căng tức vùng bụng nghi ngờ xoắn ruột', 'Abdominal distension with suspected intestinal torsion'),
  ('đau bụng cấp dữ dội (severe colic)', 'Severe colic (acute abdominal pain)'),
  ('tắc ruột (impaction colic)', 'Intestinal impaction (impaction colic)'),
  ('đau quặn bụng (abdominal pain)', 'Abdominal pain (colic)'),
  ('suy nhược (anorexia)', 'Anorexia (loss of appetite)'),
  ('loét dạ dày (equine gastric ulcer syndrome - EGUS)', 'Equine gastric ulcer syndrome (EGUS)'),
  ('tắc nghẽn kết tràng (colonic impaction)', 'Colonic impaction'),
  ('mất nước đường tiêu hóa', 'Gastrointestinal dehydration'),
  ('viêm đại tràng (colitis)', 'Colitis (large intestine inflammation)'),
  ('què/khập khiễng (equine lameness)', 'Equine lameness'),
  ('chấn thương cơ xương khớp', 'Musculoskeletal injury'),
  ('viêm màng đáy móng (laminitis)', 'Laminitis'),
  ('áp xe móng (hoof abscess)', 'Hoof abscess'),
  ('viêm màng đáy móng cấp (acute laminitis)', 'Acute laminitis'),
  ('viêm gân gấp ngón (tendinitis)', 'Tendinitis (flexor tendon inflammation)'),
  ('viêm gân sau (bowed tendon)', 'Bowed tendon'),
  ('viêm khớp (arthritis / synovitis)', 'Arthritis / synovitis'),
  ('hội chứng tiêu cơ vân do gắng sức (tying-up)', 'Exertional rhabdomyolysis (tying-up)'),
  ('viêm đường hô hấp trên', 'Upper respiratory tract inflammation'),
  ('viêm xoang hoặc nhiễm khuẩn Streptococcus equi', 'Sinusitis or Streptococcus equi infection (strangles)'),
  ('viêm phế quản hoặc hen suyễn ở ngựa (equine asthma)', 'Equine asthma (bronchitis / recurrent airway obstruction)'),
  ('hen ngựa (recurrent airway obstruction - RAO)', 'Recurrent airway obstruction (RAO / heaves)'),
  ('suy hô hấp cấp', 'Acute respiratory distress'),
  ('viêm phổi (pneumonia)', 'Pneumonia'),
  ('xuất huyết phổi do gắng sức (EIPH)', 'Exercise-induced pulmonary hemorrhage (EIPH)'),
  ('viêm da dị ứng do côn trùng (sweet itch)', 'Insect hypersensitivity dermatitis (sweet itch)'),
  ('nấm da (ringworm)', 'Ringworm (dermatophytosis)'),
  ('nhiễm trùng mô mềm (cellulitis)', 'Cellulitis (soft tissue infection)'),
  ('vết thương nhiễm khuẩn ngoài da', 'Infected skin wound'),
  ('sốt nhiễm trùng toàn thân hoặc nhiễm EHV', 'Systemic febrile infection or equine herpesvirus (EHV)'),
  ('sốt nhiễm trùng toàn thân', 'Systemic febrile infection'),
  ('bệnh lý thần kinh tủy sống (EPM)', 'Spinal cord disease (EPM)'),
  ('chứng mất điều hòa vận động (ataxia)', 'Locomotor ataxia'),
  ('mất nước nghiêm trọng (dehydration)', 'Severe dehydration')
) AS v(vn, en)
WHERE m.medical_term = v.vn;

-- The clinical-record-to-RAG sync (ClinicalVectorSyncListener) generated
-- Vietnamese document titles before the template was switched to English.
-- Retitle the two seeded clinical records already indexed.
UPDATE rag_knowledge_documents
SET title = 'Clinical record for horse ' || split_part(split_part(title, 'chiến mã ', 2), ' (Record', 1)
           || substring(title from ' \(Record #\d+\)')
WHERE title LIKE 'Bệnh án chiến mã %';

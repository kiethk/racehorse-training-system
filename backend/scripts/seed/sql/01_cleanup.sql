-- =====================================================================
-- 01_cleanup.sql — xóa sạch dữ liệu nghiệp vụ trước khi seed (TRUNCATE ... RESTART IDENTITY).
-- (Wipe all business data before seeding.)
--
-- Chỉ giữ lại bảng danh mục do migration nạp sẵn (vai trò, quyền, vùng cơ thể, phác đồ tiêm, từ điển triệu chứng,
-- kho kiến thức AI) và lịch sử Flyway. Mọi bảng khác — kể cả bảng mới ai đó thêm sau này — đều bị xóa.
-- Thêm bảng danh mục mới bằng migration thì PHẢI thêm tên bảng vào v_keep bên dưới, nếu không seed sẽ xóa nó.
-- (Only migration-seeded catalogue tables and the Flyway history are kept. Every other table, including tables added
-- later, is wiped. A new catalogue table seeded by a migration MUST be added to v_keep below.)
-- =====================================================================
DO $$
DECLARE
    v_keep TEXT[] := ARRAY[
        'flyway_schema_history',
        'roles', 'permissions', 'role_permissions',
        'horse_body_regions', 'preventive_protocols', 'medical_symptom_mappings',
        'rag_knowledge_documents', 'rag_knowledge_embeddings'
    ];
    v_tables TEXT;
BEGIN
    SELECT string_agg(format('%I.%I', schemaname, tablename), ', ' ORDER BY tablename) INTO v_tables
      FROM pg_tables
     WHERE schemaname = 'public' AND tablename <> ALL (v_keep);
    EXECUTE 'TRUNCATE ' || v_tables || ' RESTART IDENTITY CASCADE';
    RAISE NOTICE 'Cleaned every business table, kept: %', array_to_string(v_keep, ', ');
END $$;

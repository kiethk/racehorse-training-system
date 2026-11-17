M02 RELATIVE-VALUE HANDOFF PACKAGE
==================================

Dùng package này để chuyển sang WSL2 / agent khác và chạy lại 67 case M02
với giá trị tương đối.

Đọc trước
---------
plan/M02_NEW_AGENT_PROMPT.md          <- prompt copy-paste cho agent mới
plan/M02-relative-wsl2-agent-plan.md  <- plan WSL2 đầy đủ
plan/M02-relative-agent-prompts.md    <- prompt chi tiết orchestrator + A/B/C
plan/M02-new-agent-checklist.md       <- checklist cho người chuyển giao

Nội dung
--------
plan/       - plan + prompt + checklist
data/       - Excel test data, input data, long input, pure-UI workbook
seed/       - M02_test_data_seed.sql
reports/    - test plan, defect list (13 defect, UI-D08 removed)
text/       - chuỗi 2000/2001 ký tự, group name 100/101
README.txt

Shared DB
---------
Dùng chung 1 DB rtms_system_test và 1 app; không tạo DB riêng.
A/B/C chạy song song, data domains tách nhau. Nếu drift: stop agent bị ảnh hưởng,
rerun seed SQL, rerun agent đó.

Baseline
--------
67 pure-UI case TRN_TC01-TC67; 64 Pass / 3 Fail.
UI-D08 (Copilot overlay) removed/waived; 13 Jira defects.
TC53/TC62/TC63 vẫn Fail. TC58/TC61 Pass.

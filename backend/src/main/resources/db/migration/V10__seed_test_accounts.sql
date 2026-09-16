INSERT INTO users (full_name, email, password_hash, role_id, is_active)
SELECT 'Test Trainer', 'trainer@rtms.com', '$2a$10$DowJonEwHZ2fF5H8vE.Ohu6q3ZQwvSQ8pRW.rV9J8p5xoJlIYUnBS', r.id, true
FROM roles r WHERE r.name = 'HEAD_TRAINER';

INSERT INTO users (full_name, email, password_hash, role_id, is_active)
SELECT 'Test Vet', 'vet@rtms.com', '$2a$10$DowJonEwHZ2fF5H8vE.Ohu6q3ZQwvSQ8pRW.rV9J8p5xoJlIYUnBS', r.id, true
FROM roles r WHERE r.name = 'VETERINARIAN';

INSERT INTO users (full_name, email, password_hash, role_id, is_active)
SELECT 'Test Groom', 'groom@rtms.com', '$2a$10$DowJonEwHZ2fF5H8vE.Ohu6q3ZQwvSQ8pRW.rV9J8p5xoJlIYUnBS', r.id, true
FROM roles r WHERE r.name = 'GROOM';

INSERT INTO users (full_name, email, password_hash, role_id, is_active)
SELECT 'Test Owner', 'owner@rtms.com', '$2a$10$DowJonEwHZ2fF5H8vE.Ohu6q3ZQwvSQ8pRW.rV9J8p5xoJlIYUnBS', r.id, true
FROM roles r WHERE r.name = 'HORSE_OWNER';
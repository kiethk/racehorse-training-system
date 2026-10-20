-- UI chỉ hiển thị tiếng Anh: tên phác đồ seed ở V202610181000 đổi sang tiếng Anh.
UPDATE preventive_protocols SET name = 'Equine Influenza', updated_at = NOW() WHERE code = 'EI';
UPDATE preventive_protocols SET name = 'Tetanus', updated_at = NOW() WHERE code = 'TETANUS';
UPDATE preventive_protocols SET name = 'Rabies', updated_at = NOW() WHERE code = 'RABIES';
UPDATE preventive_protocols SET name = 'Equine Encephalomyelitis (EEE/WEE)', updated_at = NOW() WHERE code = 'EEE_WEE';
UPDATE preventive_protocols SET name = 'West Nile Virus', updated_at = NOW() WHERE code = 'WEST_NILE';
UPDATE preventive_protocols SET name = 'Deworming', default_dosage = 'By body weight', updated_at = NOW() WHERE code = 'DEWORM';
UPDATE preventive_protocols SET default_dosage = '1 dose', updated_at = NOW() WHERE code <> 'DEWORM';

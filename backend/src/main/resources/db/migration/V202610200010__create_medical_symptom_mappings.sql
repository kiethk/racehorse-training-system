CREATE TABLE medical_symptom_mappings (
    id BIGSERIAL PRIMARY KEY,
    raw_symptom VARCHAR(100) NOT NULL,
    medical_term VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_symptom_mappings_active ON medical_symptom_mappings (is_active);
CREATE INDEX idx_symptom_mappings_category ON medical_symptom_mappings (category);
CREATE INDEX idx_symptom_mappings_raw ON medical_symptom_mappings (LOWER(raw_symptom));

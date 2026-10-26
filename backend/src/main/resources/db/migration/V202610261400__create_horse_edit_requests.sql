-- =====================================================================
-- V202610261200: HORSE EDIT REQUESTS
--
-- An owner may ask to change a few mutable facts of an ELIGIBLE horse (name,
-- breed, sire/dam name and UELN) and to replace or add its documents. Nothing
-- touches the official horse record until the groom of the horse's stall
-- approves the request.
--
-- Why new tables: admission documents are an immutable snapshot (V48 trigger),
-- so a horse's current documents cannot be edited in place there.
--
-- horse_edit_requests.original_* hold the values the owner saw when submitting;
-- approval refuses to apply a request whose originals no longer match (stale).
--
-- horse_edit_request_documents holds every uploaded file. A file becomes the
-- horse's official document of its type only when its request is approved
-- (is_current = TRUE); the previous current file of that type is then superseded
-- and its storage object deleted after commit. Admission files are never
-- deleted: a type without a current row falls back to the latest admission file.
-- =====================================================================

CREATE TABLE horse_edit_requests (
    id                                  BIGSERIAL PRIMARY KEY,
    horse_id                            BIGINT       NOT NULL REFERENCES horses(id),
    owner_id                            BIGINT       NOT NULL REFERENCES users(id),
    status                              VARCHAR(20)  NOT NULL,
    requested_name                      VARCHAR(255) NOT NULL,
    requested_breed                     VARCHAR(255),
    requested_sire_name                 VARCHAR(255),
    requested_sire_registration_number  VARCHAR(15),
    requested_dam_name                  VARCHAR(255),
    requested_dam_registration_number   VARCHAR(15),
    original_name                       VARCHAR(255) NOT NULL,
    original_breed                      VARCHAR(255),
    original_sire_name                  VARCHAR(255),
    original_sire_registration_number   VARCHAR(15),
    original_dam_name                   VARCHAR(255),
    original_dam_registration_number    VARCHAR(15),
    reviewer_id                         BIGINT REFERENCES users(id),
    review_feedback                     TEXT,
    reviewed_at                         TIMESTAMP,
    created_at                          TIMESTAMP    NOT NULL,
    updated_at                          TIMESTAMP    NOT NULL,
    CONSTRAINT chk_her_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_her_reject_feedback CHECK (
        status <> 'REJECTED' OR (review_feedback IS NOT NULL AND length(btrim(review_feedback)) > 0)),
    CONSTRAINT chk_her_reviewed CHECK (
        status NOT IN ('APPROVED', 'REJECTED') OR (reviewer_id IS NOT NULL AND reviewed_at IS NOT NULL))
);

-- One open request per horse, enforced even when two submissions race.
CREATE UNIQUE INDEX uq_her_one_pending_per_horse
    ON horse_edit_requests (horse_id) WHERE status = 'PENDING';
CREATE INDEX idx_her_horse_created ON horse_edit_requests (horse_id, created_at DESC);

CREATE TABLE horse_edit_request_documents (
    id                  BIGSERIAL PRIMARY KEY,
    request_id          BIGINT       NOT NULL REFERENCES horse_edit_requests(id),
    horse_id            BIGINT       NOT NULL REFERENCES horses(id),
    document_type       VARCHAR(50)  NOT NULL,
    file_url            TEXT         NOT NULL,
    original_file_name  VARCHAR(255),
    uploaded_at         TIMESTAMP    NOT NULL,
    is_current          BOOLEAN      NOT NULL DEFAULT FALSE,
    superseded_at       TIMESTAMP,
    CONSTRAINT chk_herd_document_type CHECK (document_type IN (
        'HORSE_PHOTO', 'REGISTRATION_DOCUMENT', 'PEDIGREE_CERTIFICATE', 'VACCINATION_RECORD',
        'DEWORMING_RECORD', 'HEALTH_CERTIFICATE', 'PREVIOUS_MEDICAL_RECORD', 'PREVIOUS_INJURY_RECORD')),
    CONSTRAINT uq_herd_one_file_per_type_per_request UNIQUE (request_id, document_type)
);

-- At most one official (current) file per horse and document type.
CREATE UNIQUE INDEX uq_herd_current_per_type
    ON horse_edit_request_documents (horse_id, document_type) WHERE is_current;
CREATE INDEX idx_herd_request ON horse_edit_request_documents (request_id);

INSERT INTO permissions (code, description) VALUES
    ('HORSE_EDIT_REQUEST_CREATE', 'Owner requests changes to an eligible horse and its documents'),
    ('HORSE_EDIT_REQUEST_REVIEW', 'Groom approves or rejects horse change requests for horses in their stalls')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON (r.name = 'HORSE_OWNER' AND p.code = 'HORSE_EDIT_REQUEST_CREATE')
                   OR (r.name = 'GROOM' AND p.code = 'HORSE_EDIT_REQUEST_REVIEW')
ON CONFLICT DO NOTHING;

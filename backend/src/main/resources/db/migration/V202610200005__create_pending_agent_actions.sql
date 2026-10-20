CREATE TABLE pending_agent_actions (
                                       id BIGSERIAL PRIMARY KEY,
                                       action_token VARCHAR(255) UNIQUE NOT NULL,
                                       user_id BIGINT NOT NULL REFERENCES users(id),
                                       target_role VARCHAR(50) NOT NULL,
                                       tool_name VARCHAR(100) NOT NULL,
                                       target_entity_id VARCHAR(100),
                                       diff_payload JSONB NOT NULL,
                                       status VARCHAR(20) DEFAULT 'PENDING', -- PENDING | COMMITTED | EXPIRED | REJECTED
                                       expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pending_actions_token ON pending_agent_actions(action_token);
CREATE INDEX idx_pending_actions_user_status ON pending_agent_actions(user_id, status);
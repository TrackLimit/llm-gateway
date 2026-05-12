CREATE TABLE organizations
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    org_id        BIGINT       NOT NULL REFERENCES organizations (id),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_org ON users (org_id);

CREATE TABLE api_keys
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    key_hash   VARCHAR(255) NOT NULL UNIQUE,
    key_prefix VARCHAR(16)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_user_active ON api_keys (user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_api_keys_hash ON api_keys (key_hash) WHERE revoked_at IS NULL;

CREATE TABLE usage_logs
(
    id                BIGSERIAL PRIMARY KEY,
    api_key_id        BIGINT       NOT NULL REFERENCES api_keys (id),
    provider          VARCHAR(64)  NOT NULL,
    model             VARCHAR(128) NOT NULL,
    prompt_tokens     INT          NOT NULL,
    completion_tokens INT          NOT NULL,
    latency_ms        INT          NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_usage_prompt_tokens_non_negative CHECK (prompt_tokens >= 0),
    CONSTRAINT chk_usage_completion_tokens_non_negative CHECK (completion_tokens >= 0),
    CONSTRAINT chk_usage_latency_ms_non_negative CHECK (latency_ms >= 0)
);

CREATE INDEX idx_usage_logs_key_time ON usage_logs (api_key_id, created_at DESC);

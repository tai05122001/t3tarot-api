CREATE TABLE app_user (
    id                      uuid         PRIMARY KEY,
    display_name            varchar(80),
    reversal_mode           varchar(16)  NOT NULL DEFAULT 'BLOCKED'
                            CHECK (reversal_mode IN ('OFF', 'BLOCKED', 'OPPOSITE')),
    question_retention_days smallint     NOT NULL DEFAULT 7
                            CHECK (question_retention_days BETWEEN 7 AND 30),
    status                  varchar(16)  NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'LOCKED', 'DELETED')),
    created_at              timestamptz  NOT NULL DEFAULT now(),
    updated_at              timestamptz  NOT NULL DEFAULT now(),
    deleted_at              timestamptz
);

CREATE TABLE user_identity (
    id               uuid         PRIMARY KEY,
    user_id          uuid         NOT NULL REFERENCES app_user (id),
    provider         varchar(16)  NOT NULL
                     CHECK (provider IN ('GOOGLE', 'FACEBOOK', 'EMAIL', 'PHONE')),
    provider_subject varchar(255) NOT NULL,
    email_normalized varchar(320),
    phone_e164       varchar(16),
    verified_at      timestamptz,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    last_login_at    timestamptz,
    UNIQUE (provider, provider_subject)
);
CREATE INDEX ix_user_identity_user  ON user_identity (user_id);
CREATE INDEX ix_user_identity_email ON user_identity (email_normalized) WHERE email_normalized IS NOT NULL;

CREATE TABLE password_credential (
    user_id             uuid         PRIMARY KEY REFERENCES app_user (id),
    username            varchar(32)  NOT NULL,
    username_normalized varchar(32)  NOT NULL UNIQUE,
    password_hash       varchar(255) NOT NULL,
    failed_login_count  smallint     NOT NULL DEFAULT 0 CHECK (failed_login_count >= 0),
    locked_until        timestamptz,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    updated_at          timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE auth_session (
    id                 uuid         PRIMARY KEY,
    user_id            uuid         NOT NULL REFERENCES app_user (id),
    identity_id        uuid         REFERENCES user_identity (id),
    refresh_token_hash bytea        NOT NULL UNIQUE,
    user_agent         varchar(255),
    ip                 inet,
    created_at         timestamptz  NOT NULL DEFAULT now(),
    expires_at         timestamptz  NOT NULL,
    revoked_at         timestamptz
);
CREATE INDEX ix_auth_session_user ON auth_session (user_id) WHERE revoked_at IS NULL;

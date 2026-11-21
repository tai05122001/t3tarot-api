ALTER TABLE auth_session
    ADD COLUMN previous_refresh_token_hash bytea,
    ADD COLUMN rotated_at timestamptz;

CREATE UNIQUE INDEX ux_auth_session_previous_hash
    ON auth_session (previous_refresh_token_hash) WHERE previous_refresh_token_hash IS NOT NULL;

CREATE TABLE email_verification (
    id          uuid        PRIMARY KEY,
    user_id     uuid        NOT NULL REFERENCES app_user (id),
    identity_id uuid        NOT NULL REFERENCES user_identity (id),
    token_hash  bytea       NOT NULL UNIQUE,
    created_at  timestamptz NOT NULL DEFAULT now(),
    expires_at  timestamptz NOT NULL,
    consumed_at timestamptz
);

CREATE INDEX ix_email_verification_identity ON email_verification (identity_id, created_at);

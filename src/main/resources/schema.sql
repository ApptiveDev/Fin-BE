DROP TYPE IF EXISTS user_role;

CREATE TYPE user_role AS ENUM (
    'BEFORE_AGREED',
    'BASIC_ACCESS',
    'RECOMMENDATION',
    'ADMIN'
    );

CREATE TABLE IF NOT EXISTS users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255),
                       name VARCHAR(100) NOT NULL,
                       provider VARCHAR(50) NOT NULL,
                       provider_id VARCHAR(255) NOT NULL,
                       user_role user_role NOT NULL DEFAULT 'BEFORE_AGREED',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT uq_users_provider_account
                           UNIQUE (provider, provider_id)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                token_hash VARCHAR(255) NOT NULL,
                                expires_at TIMESTAMP NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT uq_refresh_tokens_token_hash
                                    UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);


CREATE TABLE IF NOT EXISTS terms (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       content TEXT NOT NULL,
                       is_required BOOLEAN NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_terms (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            term_id BIGINT NOT NULL,
                            agreed BOOLEAN NOT NULL,
                            agreed_at TIMESTAMP NULL,

                            CONSTRAINT uq_user_id_term_id
                                UNIQUE (user_id, term_id),

                            CONSTRAINT fk_user_term_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_user_term_terms
                                FOREIGN KEY (term_id)
                                    REFERENCES terms(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT chk_user_term_agreed_at
                                CHECK (
                                    (agreed = TRUE AND agreed_at IS NOT NULL)
                                        OR
                                    (agreed = FALSE)
                                    )
);

CREATE INDEX IF NOT EXISTS idx_user_terms_term_id
    ON user_terms(term_id);
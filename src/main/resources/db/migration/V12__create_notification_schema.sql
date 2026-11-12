-- V11: 이메일 알림 발송 스키마

-- users 테이블에 email_verified 컬럼 추가
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- 1. 이메일 인증 토큰
CREATE TABLE email_verification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    resend_count INT NOT NULL DEFAULT 0,
    last_sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_verification_user_id ON email_verification(user_id);
CREATE INDEX idx_email_verification_token_hash ON email_verification(token_hash);

-- 2. 전체 수신거부 상태 (이메일 단위)
CREATE TABLE notification_global (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    paused_reason VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_global_email ON notification_global(email);

-- 3. 알림 설정
CREATE TABLE notification_setting (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    notification_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, target_id, target_type, notification_type)
);

CREATE INDEX idx_notification_setting_user_id ON notification_setting(user_id);
CREATE INDEX idx_notification_setting_target ON notification_setting(target_id, target_type);

-- 4. 발송 예정
CREATE TABLE notification_schedule (
    id BIGSERIAL PRIMARY KEY,
    setting_id BIGINT NOT NULL REFERENCES notification_setting(id) ON DELETE CASCADE,
    send_date DATE NOT NULL,
    offset_label VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'scheduled',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(setting_id, send_date)
);

CREATE INDEX idx_notification_schedule_send_date ON notification_schedule(send_date);
CREATE INDEX idx_notification_schedule_status ON notification_schedule(status);

-- 5. 발송 이력
CREATE TABLE email_send_log (
    id BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT REFERENCES notification_schedule(id) ON DELETE SET NULL,
    digest_key VARCHAR(100),
    to_email VARCHAR(255) NOT NULL,
    mail_type VARCHAR(30) NOT NULL,
    resend_message_id VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_send_log_to_email ON email_send_log(to_email);
CREATE INDEX idx_email_send_log_mail_type ON email_send_log(mail_type);
CREATE INDEX idx_email_send_log_sent_at ON email_send_log(sent_at);

-- 6. 가입 상품 등록 (만기 알림용)
CREATE TABLE subscription_registration (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    term_months INT NOT NULL,
    maturity_date DATE NOT NULL,
    deposit_amount BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscription_registration_user_id ON subscription_registration(user_id);
CREATE INDEX idx_subscription_registration_maturity_date ON subscription_registration(maturity_date);

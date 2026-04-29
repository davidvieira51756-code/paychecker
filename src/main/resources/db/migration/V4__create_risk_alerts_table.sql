CREATE TABLE risk_alerts (
                             id BIGSERIAL PRIMARY KEY,

                             payment_id BIGINT NOT NULL,
                             account_id BIGINT NOT NULL,

                             risk_score INTEGER NOT NULL,
                             severity VARCHAR(20) NOT NULL,
                             status VARCHAR(30) NOT NULL,
                             reason_summary VARCHAR(500) NOT NULL,

                             created_at TIMESTAMPTZ NOT NULL,
                             updated_at TIMESTAMPTZ NOT NULL,

                             CONSTRAINT fk_risk_alerts_payment
                                 FOREIGN KEY (payment_id)
                                     REFERENCES payments(id),

                             CONSTRAINT fk_risk_alerts_account
                                 FOREIGN KEY (account_id)
                                     REFERENCES accounts(id),

                             CONSTRAINT chk_risk_alerts_score_range
                                 CHECK (risk_score >= 0 AND risk_score <= 100),

                             CONSTRAINT chk_risk_alerts_severity
                                 CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),

                             CONSTRAINT chk_risk_alerts_status
                                 CHECK (status IN ('OPEN', 'IN_REVIEW', 'FALSE_POSITIVE', 'CONFIRMED_FRAUD', 'CLOSED'))
);

CREATE INDEX idx_risk_alerts_payment_id
    ON risk_alerts(payment_id);

CREATE INDEX idx_risk_alerts_account_id
    ON risk_alerts(account_id);

CREATE INDEX idx_risk_alerts_status
    ON risk_alerts(status);

CREATE INDEX idx_risk_alerts_created_at
    ON risk_alerts(created_at);
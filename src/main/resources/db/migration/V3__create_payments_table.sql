CREATE TABLE payments (
                          id BIGSERIAL PRIMARY KEY,

                          account_id BIGINT NOT NULL,

                          amount NUMERIC(19, 2) NOT NULL,
                          currency VARCHAR(3) NOT NULL,

                          beneficiary_iban VARCHAR(34) NOT NULL,
                          beneficiary_name VARCHAR(150) NOT NULL,
                          beneficiary_country VARCHAR(2) NOT NULL,

                          status VARCHAR(30) NOT NULL,
                          decision_reason VARCHAR(500),
                          risk_score INTEGER NOT NULL,

                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,

                          CONSTRAINT fk_payments_account
                              FOREIGN KEY (account_id)
                                  REFERENCES accounts(id),

                          CONSTRAINT chk_payments_amount_positive
                              CHECK (amount > 0),

                          CONSTRAINT chk_payments_risk_score_range
                              CHECK (risk_score >= 0 AND risk_score <= 100),

                          CONSTRAINT chk_payments_status
                              CHECK (status IN ('PENDING', 'APPROVED', 'DECLINED', 'MANUAL_REVIEW', 'CANCELLED'))
);

CREATE INDEX idx_payments_account_id
    ON payments(account_id);

CREATE INDEX idx_payments_status
    ON payments(status);

CREATE INDEX idx_payments_created_at
    ON payments(created_at);
CREATE TABLE accounts (
                          id BIGSERIAL PRIMARY KEY,

                          owner_name VARCHAR(150) NOT NULL,
                          iban VARCHAR(34) NOT NULL UNIQUE,
                          currency VARCHAR(3) NOT NULL,

                          balance NUMERIC(19, 2) NOT NULL,
                          daily_limit NUMERIC(19, 2) NOT NULL,
                          monthly_limit NUMERIC(19, 2) NOT NULL,

                          status VARCHAR(20) NOT NULL,

                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,

                          CONSTRAINT chk_accounts_balance_non_negative
                              CHECK (balance >= 0),

                          CONSTRAINT chk_accounts_daily_limit_non_negative
                              CHECK (daily_limit >= 0),

                          CONSTRAINT chk_accounts_monthly_limit_non_negative
                              CHECK (monthly_limit >= 0),

                          CONSTRAINT chk_accounts_status
                              CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED'))
);
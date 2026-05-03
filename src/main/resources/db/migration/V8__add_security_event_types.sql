ALTER TABLE financial_events
DROP CONSTRAINT chk_financial_events_type;

ALTER TABLE financial_events
    ADD CONSTRAINT chk_financial_events_type
        CHECK (event_type IN (
                              'ACCOUNT_CREATED',
                              'PAYMENT_REQUESTED',
                              'PAYMENT_APPROVED',
                              'PAYMENT_DECLINED',
                              'PAYMENT_SENT_TO_REVIEW',
                              'RISK_SCORE_CALCULATED',
                              'RISK_ALERT_CREATED',
                              'RISK_ALERT_STATUS_UPDATED',
                              'LOGIN_SUCCESS',
                              'LOGIN_FAILED'
            ));
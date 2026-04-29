CREATE TABLE financial_events (
                                  id BIGSERIAL PRIMARY KEY,

                                  event_type VARCHAR(60) NOT NULL,
                                  entity_type VARCHAR(60) NOT NULL,
                                  entity_id BIGINT NOT NULL,

                                  payload_json TEXT NOT NULL,

                                  created_by VARCHAR(100) NOT NULL,
                                  created_at TIMESTAMPTZ NOT NULL,

                                  CONSTRAINT chk_financial_events_type
                                      CHECK (event_type IN (
                                                            'ACCOUNT_CREATED',
                                                            'PAYMENT_REQUESTED',
                                                            'PAYMENT_APPROVED',
                                                            'PAYMENT_DECLINED',
                                                            'PAYMENT_SENT_TO_REVIEW',
                                                            'RISK_SCORE_CALCULATED',
                                                            'RISK_ALERT_CREATED'
                                          ))
);

CREATE INDEX idx_financial_events_event_type
    ON financial_events(event_type);

CREATE INDEX idx_financial_events_entity
    ON financial_events(entity_type, entity_id);

CREATE INDEX idx_financial_events_created_at
    ON financial_events(created_at);
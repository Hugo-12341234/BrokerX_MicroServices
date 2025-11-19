-- Migration pour créer la table outbox_events pour l'outbox pattern
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT
);

-- Index pour améliorer les performances des requêtes de traitement
CREATE INDEX idx_outbox_events_processed ON outbox_events (processed_at);
CREATE INDEX idx_outbox_events_retry ON outbox_events (next_retry_at) WHERE processed_at IS NULL;
CREATE INDEX idx_outbox_events_created ON outbox_events (created_at);

CREATE TABLE analytics_events
(
    id           BIGSERIAL PRIMARY KEY,
    occurred_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    event_type   VARCHAR(64)            NOT NULL,
    path         VARCHAR(1024)          NOT NULL,
    session_id   UUID,
    user_id      BIGINT,
    ip_hash      VARCHAR(128),
    user_agent   VARCHAR(512)
);

CREATE INDEX idx_analytics_occurred_at ON analytics_events (occurred_at);
CREATE INDEX idx_analytics_event_type ON analytics_events (event_type);
CREATE INDEX idx_analytics_path ON analytics_events (path);

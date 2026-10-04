CREATE INDEX IF NOT EXISTS idx_analytics_events_user_id ON analytics_events (user_id);
CREATE INDEX IF NOT EXISTS idx_analytics_events_event_type ON analytics_events (event_type);

ALTER TABLE analytics_events
    ALTER COLUMN user_id TYPE UUID USING NULL;

ALTER TABLE games
    ADD COLUMN metadata_synced_at TIMESTAMPTZ,
    ADD COLUMN metadata_sync_attempted_at TIMESTAMPTZ,
    ADD COLUMN metadata_sync_status VARCHAR(40),
    ADD COLUMN metadata_sync_error TEXT;

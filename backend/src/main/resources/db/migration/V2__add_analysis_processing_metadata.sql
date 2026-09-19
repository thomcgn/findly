ALTER TABLE analysis
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN progress INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN started_at TIMESTAMPTZ,
    ADD COLUMN failed_at TIMESTAMPTZ,
    ADD COLUMN error_code VARCHAR(128),
    ADD COLUMN error_message VARCHAR(2048);

UPDATE analysis SET progress = 100 WHERE status IN ('COMPLETED', 'FAILED');

ALTER TABLE analysis
    ADD CONSTRAINT analysis_progress_range CHECK (progress BETWEEN 0 AND 100);

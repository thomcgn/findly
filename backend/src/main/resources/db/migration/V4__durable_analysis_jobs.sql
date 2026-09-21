ALTER TABLE analysis ADD COLUMN source_url VARCHAR(2048), ADD COLUMN deadline_at TIMESTAMPTZ;
UPDATE analysis a SET source_url = l.external_url FROM listing l WHERE l.analysis_id = a.id;
UPDATE analysis SET status = 'CREATED' WHERE status = 'PENDING';
UPDATE analysis SET status = 'FETCHING_LISTING' WHERE status = 'ANALYZING';
UPDATE analysis SET deadline_at = created_at + INTERVAL '5 minutes'
WHERE status NOT IN ('COMPLETED', 'FAILED');
CREATE INDEX idx_analysis_active_deadline ON analysis (deadline_at)
WHERE status NOT IN ('COMPLETED', 'FAILED');

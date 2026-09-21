-- Earlier versions persisted heuristic products and synthetic comparison prices.
-- Keep historical rows for audit, but never present them as verified results.
INSERT INTO analysis_warnings (analysis_id, warning)
SELECT id, 'LEGACY_RESULT_UNVERIFIED' FROM analysis WHERE status = 'COMPLETED';

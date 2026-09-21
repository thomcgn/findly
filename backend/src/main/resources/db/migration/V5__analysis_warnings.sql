CREATE TABLE analysis_warnings (
    analysis_id UUID NOT NULL REFERENCES analysis(id) ON DELETE CASCADE,
    warning VARCHAR(2048) NOT NULL
);
CREATE INDEX idx_analysis_warnings_analysis ON analysis_warnings (analysis_id);

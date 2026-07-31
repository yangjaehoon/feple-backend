ALTER TABLE artist   ADD COLUMN deleted_at DATETIME NULL;
ALTER TABLE festival ADD COLUMN deleted_at DATETIME NULL;

CREATE INDEX idx_artist_deleted_at   ON artist(deleted_at);
CREATE INDEX idx_festival_deleted_at ON festival(deleted_at);

ALTER TABLE library_entries
    ALTER COLUMN started_at TYPE DATE USING started_at::date,
    ALTER COLUMN completed_at TYPE DATE USING completed_at::date;

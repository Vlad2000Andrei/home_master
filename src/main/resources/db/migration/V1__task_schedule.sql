CREATE TABLE IF NOT EXISTS task_schedule (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES task (id),
    repeat_rate TEXT NOT NULL,
    scheduled_time TIME NOT NULL
);
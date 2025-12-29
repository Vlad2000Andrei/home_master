CREATE TABLE
    IF NOT EXISTS task (id BIGSERIAL PRIMARY KEY, name TEXT NOT NULL);

CREATE TABLE IF NOT EXISTS task_schedule (
        id BIGSERIAL PRIMARY KEY,
        task_id BIGINT NOT NULL REFERENCES task (id),
        repeat_rate TEXT NOT NULL,
        scheduled_time TIME NOT NULL,
        start_date DATE NOT NULL,
        name TEXT
    );

CREATE TABLE
    IF NOT EXISTS task_completion (
        id BIGSERIAL PRIMARY KEY,
        task_id BIGINT NOT NULL references task (id),
        time_due TIMESTAMP NOT NULL,
        time_completed TIMESTAMP,
        task_schedule_id BIGINT NOT NULL references task_schedule (id)
    );

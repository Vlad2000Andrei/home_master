CREATE TABLE
    IF NOT EXISTS task (id BIGSERIAL PRIMARY KEY, name TEXT NOT NULL);

CREATE TABLE
    IF NOT EXISTS task_completion (
        id BIGSERIAL PRIMARY KEY,
        task_id BIGINT NOT NULL references task (id),
        time_completed TIMESTAMP NOT NULL
    );
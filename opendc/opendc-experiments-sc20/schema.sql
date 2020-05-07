DROP TABLE IF EXISTS host_reports;
CREATE TABLE host_reports (
    id                      BIGSERIAL PRIMARY KEY   NOT NULL,
    experiment_id           BIGINT                  NOT NULL,
    time                    BIGINT                  NOT NULL,
    duration                BIGINT                  NOT NULL,
    requested_burst         BIGINT                  NOT NULL,
    granted_burst           BIGINT                  NOT NULL,
    overcommissioned_burst  BIGINT                  NOT NULL,
    interfered_burst        BIGINT                  NOT NULL,
    cpu_usage               DOUBLE PRECISION        NOT NULL,
    cpu_demand              DOUBLE PRECISION        NOT NULL,
    image_count             INTEGER                 NOT NULL,
    server                  TEXT                    NOT NULL,
    host_state              TEXT                    NOT NULL,
    host_usage              DOUBLE PRECISION        NOT NULL,
    power_draw              DOUBLE PRECISION        NOT NULL,
    total_submitted_vms     BIGINT                  NOT NULL,
    total_queued_vms        BIGINT                  NOT NULL,
    total_running_vms       BIGINT                  NOT NULL,
    total_finished_vms      BIGINT                  NOT NULL
);

-- An experiment represents a collection of portfolios.
DROP TABLE IF EXISTS experiments CASCADE;
CREATE TABLE experiments
(
    id            BIGSERIAL PRIMARY KEY NOT NULL,
    creation_time TIMESTAMP             NOT NULL DEFAULT (now())
);

-- A portfolio represents a collection of scenarios tested.
DROP TABLE IF EXISTS portfolios CASCADE;
CREATE TABLE portfolios
(
    id            BIGSERIAL PRIMARY KEY NOT NULL,
    experiment_id BIGINT                NOT NULL,
    name          TEXT                  NOT NULL,

    FOREIGN KEY (experiment_id) REFERENCES experiments (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- A scenario represents a single point in the design space (a unique combination of parameters)
DROP TABLE IF EXISTS scenarios CASCADE;
CREATE TABLE scenarios
(
    id                BIGSERIAL PRIMARY KEY NOT NULL,
    portfolio_id      BIGINT                NOT NULL,
    repetitions       INTEGER               NOT NULL,
    topology          TEXT                  NOT NULL,
    workload_name     TEXT                  NOT NULL,
    workload_fraction DOUBLE PRECISION      NOT NULL,
    allocation_policy TEXT                  NOT NULL,
    failures          BOOLEAN               NOT NULL,
    interference      BOOLEAN               NOT NULL,

    FOREIGN KEY (portfolio_id) REFERENCES portfolios (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE

);

DROP TYPE IF EXISTS run_state CASCADE;
CREATE TYPE run_state AS ENUM ('wait', 'active', 'fail', 'ok');

-- An experiment run represent a single invocation of a trial and is used to distinguish between repetitions of the
-- same set of parameters.
DROP TABLE IF EXISTS runs CASCADE;
CREATE TABLE runs
(
    id          INTEGER   NOT NULL,
    scenario_id BIGINT    NOT NULL,
    seed        INTEGER   NOT NULL,
    state       run_state NOT NULL DEFAULT 'wait'::run_state,
    submit_time TIMESTAMP NOT NULL DEFAULT (now()),
    start_time  TIMESTAMP DEFAULT (NULL),
    end_time    TIMESTAMP DEFAULT (NULL),

    PRIMARY KEY (scenario_id, id),
    FOREIGN KEY (scenario_id) REFERENCES scenarios (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- Metrics of the hypervisors reported per slice
DROP TABLE IF EXISTS host_metrics CASCADE;
CREATE TABLE host_metrics
(
    id                     BIGSERIAL PRIMARY KEY NOT NULL,
    scenario_id            BIGINT                NOT NULL,
    run_id                 INTEGER               NOT NULL,
    host_id                TEXT                  NOT NULL,
    state                  TEXT                  NOT NULL,
    timestamp              TIMESTAMP             NOT NULL,
    duration               BIGINT                NOT NULL,
    vm_count               INTEGER               NOT NULL,
    requested_burst        BIGINT                NOT NULL,
    granted_burst          BIGINT                NOT NULL,
    overcommissioned_burst BIGINT                NOT NULL,
    interfered_burst       BIGINT                NOT NULL,
    cpu_usage              DOUBLE PRECISION      NOT NULL,
    cpu_demand             DOUBLE PRECISION      NOT NULL,
    power_draw             DOUBLE PRECISION      NOT NULL,

    FOREIGN KEY (scenario_id, run_id) REFERENCES runs (scenario_id, id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

DROP INDEX IF EXISTS host_metrics_idx;
CREATE INDEX host_metrics_idx ON host_metrics (scenario_id, run_id, timestamp, host_id);

-- Metrics of the VMs reported per slice
DROP TABLE IF EXISTS vm_metrics CASCADE;
CREATE TABLE vm_metrics
(
    id                     BIGSERIAL PRIMARY KEY NOT NULL,
    scenario_id            BIGINT                NOT NULL,
    run_id                 INTEGER               NOT NULL,
    vm_id                  TEXT                  NOT NULL,
    host_id                TEXT                  NOT NULL,
    state                  TEXT                  NOT NULL,
    timestamp              TIMESTAMP             NOT NULL,
    duration               BIGINT                NOT NULL,
    requested_burst        BIGINT                NOT NULL,
    granted_burst          BIGINT                NOT NULL,
    overcommissioned_burst BIGINT                NOT NULL,
    interfered_burst       BIGINT                NOT NULL,
    cpu_usage              DOUBLE PRECISION      NOT NULL,
    cpu_demand             DOUBLE PRECISION      NOT NULL,

    FOREIGN KEY (scenario_id, run_id) REFERENCES runs (scenario_id, id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

DROP INDEX IF EXISTS vm_metrics_idx;
CREATE INDEX vm_metrics_idx ON vm_metrics (scenario_id, run_id, timestamp, vm_id);

-- Metrics of the provisioner reported per change
DROP TABLE IF EXISTS provisioner_metrics CASCADE;
CREATE TABLE provisioner_metrics
(
    id                   BIGSERIAL PRIMARY KEY NOT NULL,
    scenario_id          BIGINT                NOT NULL,
    run_id               INTEGER               NOT NULL,
    timestamp            TIMESTAMP             NOT NULL,
    host_total_count     INTEGER               NOT NULL,
    host_available_count INTEGER               NOT NULL,
    vm_total_count       INTEGER               NOT NULL,
    vm_active_count      INTEGER               NOT NULL,
    vm_inactive_count    INTEGER               NOT NULL,
    vm_waiting_count     INTEGER               NOT NULL,
    vm_failed_count      INTEGER               NOT NULL,

    FOREIGN KEY (scenario_id, run_id) REFERENCES runs (scenario_id, id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

DROP INDEX IF EXISTS provisioner_metrics_idx;
CREATE INDEX provisioner_metrics_idx ON provisioner_metrics (scenario_id, run_id, timestamp);

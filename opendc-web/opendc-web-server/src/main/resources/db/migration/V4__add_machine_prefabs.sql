ALTER TABLE projects ADD COLUMN machine_prefabs_created INTEGER NOT NULL DEFAULT 0;

CREATE SEQUENCE machine_prefab_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE machine_prefabs (
    id BIGINT NOT NULL DEFAULT nextval('machine_prefab_id_seq'),
    project_id BIGINT NOT NULL REFERENCES projects(id),
    number INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    machine JSONB NOT NULL,
    CONSTRAINT pk_machine_prefabs PRIMARY KEY (id),
    CONSTRAINT uk_machine_prefabs_number UNIQUE (project_id, number)
);

CREATE INDEX ux_machine_prefabs_number ON machine_prefabs (project_id, number);

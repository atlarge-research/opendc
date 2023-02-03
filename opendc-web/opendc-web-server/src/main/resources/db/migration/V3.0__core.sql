-- Hibernate sequence for unique identifiers
create sequence hibernate_sequence start with 1 increment by 1;

-- Projects
create table projects
(
    id                 bigint       not null,
    created_at         timestamp    not null,
    name               varchar(255) not null,
    portfolios_created integer      not null default 0,
    scenarios_created  integer      not null default 0,
    topologies_created integer      not null default 0,
    updated_at         timestamp    not null,
    primary key (id)
);

create type project_role as enum ('OWNER', 'EDITOR', 'VIEWER');

-- Project authorizations authorize users specific permissions to a project.
create table project_authorizations
(
    project_id bigint       not null,
    user_id    varchar(255) not null,
    role       project_role not null,
    primary key (project_id, user_id)
);

-- Topologies represent the datacenter designs created by users.
create table topologies
(
    id         bigint       not null,
    created_at timestamp    not null,
    name       varchar(255) not null,
    number     integer      not null,
    rooms      jsonb        not null,
    updated_at timestamp    not null,
    project_id bigint       not null,
    primary key (id)
);

-- Portfolios
create table portfolios
(
    id         bigint       not null,
    name       varchar(255) not null,
    number     integer      not null,
    targets    jsonb        not null,
    project_id bigint       not null,
    primary key (id)
);

create table scenarios
(
    id                bigint           not null,
    name              varchar(255)     not null,
    number            integer          not null,
    phenomena         jsonb            not null,
    scheduler_name    varchar(255)     not null,
    sampling_fraction double precision not null,
    portfolio_id      bigint           not null,
    project_id        bigint           not null,
    topology_id       bigint           not null,
    trace_id          varchar(255)     not null,
    primary key (id)
);

create type job_state as enum ('PENDING', 'CLAIMED', 'RUNNING', 'FINISHED', 'FAILED');

create table jobs
(
    id          bigint       not null,
    created_by  varchar(255) not null,
    created_at  timestamp    not null,
    repeats     integer      not null,
    results     jsonb,
    state       job_state    not null default 'PENDING',
    runtime     integer      not null default 0,
    updated_at  timestamp    not null,
    scenario_id bigint       not null,
    primary key (id)
);

-- User accounting
create table user_accounting
(
    user_id                varchar(255) not null,
    period_end             date         not null,
    simulation_time        integer      not null,
    simulation_time_budget integer      not null,
    primary key (user_id)
);

-- Workload traces available to the user.
create table traces
(
    id   varchar(255) not null,
    name varchar(255) not null,
    type varchar(255) not null,
    primary key (id)
);

-- Relations
alter table project_authorizations
    add constraint fk_project_authorizations
        foreign key (project_id)
            references projects;

create index ux_topologies_number on topologies (project_id, number);

alter table topologies
    add constraint uk_topologies_number unique (project_id, number);

alter table topologies
    add constraint fk_topologies_project
        foreign key (project_id)
            references projects;

create index ux_portfolios_number on portfolios (project_id, number);

alter table portfolios
    add constraint fk_portfolios_project
        foreign key (project_id)
            references projects;

alter table portfolios
    add constraint uk_portfolios_number unique (project_id, number);

create index ux_scenarios_number on scenarios (project_id, number);

alter table scenarios
    add constraint uk_scenarios_number unique (project_id, number);

alter table scenarios
    add constraint fk_scenarios_project
        foreign key (project_id)
            references projects;

alter table scenarios
    add constraint fk_scenarios_topology
        foreign key (topology_id)
            references topologies;

alter table scenarios
    add constraint fk_scenarios_portfolio
        foreign key (portfolio_id)
            references portfolios;

alter table scenarios
    add constraint fk_scenarios_trace
        foreign key (trace_id)
            references traces;

alter table jobs
    add constraint fk_scenarios_job
        foreign key (scenario_id)
            references scenarios;

-- Initial data
insert into traces (id, name, type)
values ('bitbrains-small', 'Bitbrains Small', 'vm');

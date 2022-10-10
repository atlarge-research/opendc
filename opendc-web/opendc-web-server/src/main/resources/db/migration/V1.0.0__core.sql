-- Hibernate sequence for unique identifiers
create sequence hibernate_sequence start with 1 increment by 1;

-- Projects
create table projects
(
    id                 bigint       not null,
    created_at         timestamp    not null,
    name               varchar(255) not null,
    portfolios_created integer      not null,
    scenarios_created  integer      not null,
    topologies_created integer      not null,
    updated_at         timestamp    not null,
    primary key (id)
);

-- Project authorizations authorize users specific permissions to a project.
create table project_authorizations
(
    project_id bigint       not null,
    user_id    varchar(255) not null,
    role       integer      not null,
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
    job_id            bigint,
    portfolio_id      bigint           not null,
    project_id        bigint           not null,
    topology_id       bigint           not null,
    trace_id          varchar(255)     not null,
    primary key (id)
);

create table jobs
(
    id         bigint       not null,
    created_by varchar(255) not null,
    created_at timestamp    not null,
    repeats    integer      not null,
    results    jsonb,
    state      integer      not null,
    runtime    integer      not null,
    updated_at timestamp    not null,
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
    add constraint FK824hw0npe6gwiamwb6vohsu19
        foreign key (project_id)
            references projects;

create index fn_topologies_number on topologies (project_id, number);

alter table topologies
    add constraint UK2s5na63qtu2of4g7odocmwi2a unique (project_id, number);

alter table topologies
    add constraint FK1kpw87pylq7m2ct9lq0ed1u3b
        foreign key (project_id)
            references projects;

create index fn_portfolios_number on portfolios (project_id, number);

alter table portfolios
    add constraint FK31ytuaxb7aboxueng9hq7owwa
        foreign key (project_id)
            references projects;

alter table portfolios
    add constraint UK56dtskxruwj22dvxny2hfhks1 unique (project_id, number);

create index fn_scenarios_number on scenarios (project_id, number);

alter table scenarios
    add constraint UKd0bk6fmtw5qiu9ty7t3g9crqd unique (project_id, number);

alter table scenarios
    add constraint FK9utvg0i5uu8db9pa17a1d77iy
        foreign key (job_id)
            references jobs;

alter table scenarios
    add constraint FK181y5hv0uibhj7fpbpkdy90s5
        foreign key (portfolio_id)
            references portfolios;

alter table scenarios
    add constraint FKbvwyh4joavs444rj270o3b8fr
        foreign key (project_id)
            references projects;

alter table scenarios
    add constraint FKrk6ltvaf9lp0aukp9dq3qjujj
        foreign key (topology_id)
            references topologies;

alter table scenarios
    add constraint FK5m05tqeekqjkbbsaj3ehl6o8n
        foreign key (trace_id)
            references traces;

-- Initial data
insert into traces (id, name, type)
values ('bitbrains-small', 'Bitbrains Small', 'vm');

-- Test entities

alter sequence projects_SEQ restart with 500;
alter sequence portfolios_SEQ restart with 500;
alter sequence topologies_SEQ restart with 500;
alter sequence scenarios_SEQ restart with 500;
alter sequence jobs_SEQ restart with 500;

insert into projects (id, created_at, name, portfolios_created, scenarios_created, topologies_created, updated_at)
values (1, current_timestamp(), 'Test Project', 1, 2, 1, current_timestamp());
insert into project_authorizations (project_id, user_id, role)
values (1, 'owner', 'OWNER'),
       (1, 'editor', 'EDITOR'),
       (1, 'viewer', 'VIEWER');

insert into portfolios (id, name, number, targets, project_id)
values (1, 'Test Portfolio', 1, '{ "metrics": [] }' format json, 1);

insert into topologies (id, created_at, name, number, rooms, updated_at, project_id)
values (1, current_timestamp(), 'Test Topology', 1, '[]' format json, current_timestamp(), 1);

insert into scenarios (id, name, number, phenomena, scheduler_name, sampling_fraction, portfolio_id, project_id, topology_id, trace_id)
values (1, 'Test Scenario', 1, '{ "failures": false, "interference": false }' format json, 'mem', 1.0, 1, 1, 1, 'bitbrains-small'),
       (2, 'Test Scenario', 2, '{ "failures": false, "interference": false }' format json, 'mem', 1.0, 1, 1, 1, 'bitbrains-small');

insert into jobs (id, created_by, created_at, repeats, updated_at, scenario_id)
values (1, 'owner', current_timestamp(), 1, current_timestamp(), 1),
       (2, 'owner', current_timestamp(), 1, current_timestamp(), 2);

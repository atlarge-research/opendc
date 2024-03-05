
-- Insert data

INSERT INTO PROJECT (created_at, name, portfolios_created, scenarios_created, topologies_created, updated_at, id)
       VALUES ('2024-03-01T15:31:41.579969Z', 'Test Project 1', 0, 0, 0, '2024-03-01T15:31:41.579969Z', 1);

INSERT INTO PROJECTAUTHORIZATION (role, project_id, user_name)
VALUES ('OWNER', 1, 'test_user_1');

-- Add test user 2 as a viewer for project 1

INSERT INTO PROJECTAUTHORIZATION (role, project_id, user_name)
VALUES ('VIEWER', 1, 'test_user_2');

-- Add test user 3 as an editor for project 1

INSERT INTO PROJECTAUTHORIZATION (role, project_id, user_name)
VALUES ('EDITOR', 1, 'test_user_3');

-- Create a project for test user 2

INSERT INTO PROJECT (created_at, name, portfolios_created, scenarios_created, topologies_created, updated_at, id)
VALUES ('2024-03-01T15:31:41.579969Z', 'Test Project 2', 0, 0, 0, '2024-03-01T15:31:41.579969Z', 2);

INSERT INTO PROJECTAUTHORIZATION (role, project_id, user_name)
VALUES ('OWNER', 2, 'test_user_2');

-- Create three projects for test user 3. User 3 has multiple projects to test getAll

INSERT INTO PROJECT (created_at, name, portfolios_created, scenarios_created, topologies_created, updated_at, id)
VALUES ('2024-03-01T15:31:41.579969Z', 'Test Project 3', 0, 0, 0, '2024-03-01T15:31:41.579969Z', 3);

INSERT INTO PROJECTAUTHORIZATION (role, project_id, user_name)
VALUES ('OWNER', 3, 'test_user_3');

INSERT INTO PROJECT (created_at, name, portfolios_created, scenarios_created, topologies_created, updated_at, id)
VALUES ('2024-03-01T15:31:41.579969Z', 'Test Project 4', 0, 0, 0, '2024-03-01T15:31:41.579969Z', 4);

INSERT INTO PROJECTAUTHORIZATION (role, project_id, user_name)
VALUES ('OWNER', 4, 'test_user_3');

INSERT INTO PROJECT (created_at, name, portfolios_created, scenarios_created, topologies_created, updated_at, id)
VALUES ('2024-03-01T15:31:41.579969Z', 'Test Project 5', 0, 0, 0, '2024-03-01T15:31:41.579969Z', 5);

INSERT INTO PROJECTAUTHORIZATION (role, project_id, user_name)
VALUES ('OWNER', 5, 'test_user_3');

-- Project to delete

INSERT INTO PROJECT (created_at, name, portfolios_created, scenarios_created, topologies_created, updated_at, id)
VALUES ('2024-03-01T15:31:41.579969Z', 'Test Project Delete', 0, 0, 0, '2024-03-01T15:31:41.579969Z', 6);

INSERT INTO PROJECTAUTHORIZATION (role, project_id, user_name)
VALUES ('OWNER', 6, 'test_user_1');

-- --------------------------------------------------------------------------------
--  PortFolios
-- --------------------------------------------------------------------------------

-- Add Portfolio to project 1
INSERT INTO PORTFOLIO (name, number, project_id, targets, id)
VALUES ('Test PortFolio Base', 1, 1, '{"metrics": [], "repeats":1}' FORMAT JSON, 1);

INSERT INTO PORTFOLIO (name, number, project_id, targets, id)
VALUES ('Test PortFolio Delete', 2, 1, '{"metrics": [], "repeats":1}' FORMAT JSON, 2);

INSERT INTO PORTFOLIO (name, number, project_id, targets, id)
VALUES ('Test PortFolio DeleteEditor', 3, 1, '{"metrics": [], "repeats":1}' FORMAT JSON, 3);

UPDATE Project p
SET p.portfolios_created = 3, p.updated_at = '2024-03-01T15:31:41.579969Z'
WHERE p.id = 1;

-- --------------------------------------------------------------------------------
--  Topologies
-- --------------------------------------------------------------------------------

INSERT INTO TOPOLOGY (created_at, name, number, project_id, rooms, updated_at, id)
VALUES ('2024-03-01T15:31:41.579969Z', 'Test Topology testUpdate', 1, 1, '[]' FORMAT JSON, '2024-03-01T15:31:41.579969Z', 1);

INSERT INTO TOPOLOGY (created_at, name, number, project_id, rooms, updated_at, id)
VALUES ('2024-03-01T15:31:41.579969Z', 'Test Topology testDeleteAsEditor', 2, 1, '[]' FORMAT JSON, '2024-03-01T15:31:41.579969Z', 2);

INSERT INTO TOPOLOGY (created_at, name, number, project_id, rooms, updated_at, id)
VALUES ('2024-03-01T15:31:41.579969Z', 'Test Topology testDelete', 3, 1, '[]' FORMAT JSON, '2024-03-01T15:31:41.579969Z', 3);

INSERT INTO TOPOLOGY (created_at, name, number, project_id, rooms, updated_at, id)
VALUES ('2024-03-01T15:31:41.579969Z', 'Test Topology testDeleteUsed', 4, 1, '[]' FORMAT JSON, '2024-03-01T15:31:41.579969Z', 4);

UPDATE Project p
SET p.topologies_created = 4, p.updated_at = '2024-03-01T15:31:41.579969Z'
WHERE p.id = 1;

-- --------------------------------------------------------------------------------
--  Traces
-- --------------------------------------------------------------------------------

INSERT INTO TRACE (id, name, type)
VALUES ('bitbrains-small', 'Bitbrains Small', 'small');

-- --------------------------------------------------------------------------------
--  Scenario
-- --------------------------------------------------------------------------------

INSERT INTO SCENARIO (name, number, phenomena, portfolio_id, project_id, scheduler_name, topology_id, sampling_fraction, trace_id, id)
VALUES ('Test Scenario testDelete', 1, '{"failures": false, "interference": false}' FORMAT JSON, 1, 1, 'test', 1, 1.0, 'bitbrains-small', 1);

INSERT INTO SCENARIO (name, number, phenomena, portfolio_id, project_id, scheduler_name, topology_id, sampling_fraction, trace_id, id)
VALUES ('Test Scenario testDeleteUsed', 2, '{"failures": false, "interference": false}' FORMAT JSON, 1, 1, 'test', 4, 1.0, 'bitbrains-small', 2);


UPDATE Project p
SET p.scenarios_created = 2, p.updated_at = '2024-03-01T15:31:41.579969Z'
WHERE p.id = 1;

-- --------------------------------------------------------------------------------
--  Job
-- --------------------------------------------------------------------------------

INSERT INTO JOB (scenario_id, created_by, created_at, repeats, updated_at, state, runtime, results, id)
VALUES (1, 'test_user_1', '2024-03-01T15:31:41.579969Z', 1, '2024-03-01T15:31:41.579969Z', 'PENDING', 1, '{}' FORMAT JSON, 1);

INSERT INTO JOB (scenario_id, created_by, created_at, repeats, updated_at, state, runtime, results, id)
VALUES (1, 'test_user_1', '2024-03-01T15:31:41.579969Z', 1, '2024-03-01T15:31:41.579969Z', 'PENDING', 1, '{}' FORMAT JSON, 2);

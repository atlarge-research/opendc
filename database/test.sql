-- Users
INSERT INTO users (google_id, email, given_name, family_name)
VALUES ('106671218963420759042', 'l.overweel@gmail.com', 'Leon', 'Overweel');
INSERT INTO users (google_id, email, given_name, family_name)
VALUES ('118147174005839766927', 'jorgos.andreadis@gmail.com', 'Jorgos', 'Andreadis');

-- Simulations
INSERT INTO simulations (name, datetime_created, datetime_last_edited)
VALUES ('Test Simulation 1', '2016-07-11T11:00:00', '2016-07-11T11:00:00');

-- Authorizations
INSERT INTO authorizations (user_id, simulation_id, authorization_level)
VALUES (1, 1, 'OWN');
INSERT INTO authorizations (user_id, simulation_id, authorization_level)
VALUES (2, 1, 'OWN');

-- Paths
INSERT INTO paths (simulation_id, datetime_created)
VALUES (1, '2016-07-11T11:00:00');
INSERT INTO paths (simulation_id, datetime_created)
VALUES (1, '2016-07-18T09:00:00');

-- Datacenter
INSERT INTO datacenters (starred, simulation_id) VALUES (0, 1);
INSERT INTO datacenters (starred, simulation_id) VALUES (0, 1);
INSERT INTO datacenters (starred, simulation_id) VALUES (0, 1);

-- Sections
INSERT INTO sections (path_id, datacenter_id, start_tick) VALUES (1, 1, 0);
INSERT INTO sections (path_id, datacenter_id, start_tick) VALUES (1, 2, 50);
INSERT INTO sections (path_id, datacenter_id, start_tick) VALUES (1, 3, 100);

INSERT INTO sections (path_id, datacenter_id, start_tick) VALUES (2, 3, 0);

-- Default Test Trace
INSERT INTO traces (name) VALUES ('Default');

-- Jobs
INSERT INTO jobs (name, trace_id) VALUES ('Default', 1);

-- Tasks
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 400000, 1, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (25, 10000, 1, 'PARALLEL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (25, 10000, 1, 'PARALLEL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (26, 10000, 1, 'PARALLEL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (80, 200000, 1, 1, 'PARALLEL');

-- Image Processing Trace
INSERT INTO traces (name) VALUES ('Image Processing');

-- Jobs
INSERT INTO jobs (name, trace_id) VALUES ('Image Processing', 2);

-- Tasks
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (10, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (20, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (10, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (20, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (1, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (11, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (21, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (1, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (11, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (21, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (10, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (20, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (10, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (20, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (1, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (11, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (21, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (1, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (11, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (21, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (10, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (20, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (10, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (20, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (1, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (11, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (21, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (1, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (11, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (21, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (10, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (20, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (10, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (20, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (1, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (11, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (21, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (1, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (11, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (21, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (10, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (20, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (10, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (20, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (1, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (11, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (21, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (1, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (11, 100000, 2, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (21, 100000, 2, 'SEQUENTIAL');

-- Path Planning Trace
INSERT INTO traces (name) VALUES ('Path planning');

-- Jobs
INSERT INTO jobs (name, trace_id) VALUES ('Path planning', 3);

INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 1000000, 3, 'PARALLEL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (11, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (12, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (13, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (14, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (11, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (12, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (13, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (14, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (11, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (12, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (13, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (14, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (11, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (12, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (13, 200000, 3, 66, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, task_dependency_id, parallelizability)
VALUES (14, 200000, 3, 66, 'SEQUENTIAL');

-- Parallelizable Trace
INSERT INTO traces (name) VALUES ('Parallel heavy trace');

-- Jobs
INSERT INTO jobs (name, trace_id) VALUES ('Parallel heavy trace', 4);

INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 100000, 4, 'SEQUENTIAL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 900000, 4, 'PARALLEL');

-- Sequential Trace
INSERT INTO traces (name) VALUES ('Sequential heavy trace');

-- Jobs
INSERT INTO jobs (name, trace_id) VALUES ('Sequential heavy trace', 5);

INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 100000, 5, 'PARALLEL');
INSERT INTO tasks (start_tick, total_flop_count, job_id, parallelizability) VALUES (0, 900000, 5, 'SEQUENTIAL');

-- Experiments
INSERT INTO experiments (simulation_id, path_id, trace_id, scheduler_name, name, state, last_simulated_tick)
VALUES (1, 1, 1, 'SRTF', 'Default trace, SRTF', 'QUEUED', 110);
INSERT INTO experiments (simulation_id, path_id, trace_id, scheduler_name, name, state, last_simulated_tick)
VALUES (1, 1, 2, 'SRTF', 'Image processing trace, SRTF', 'QUEUED', 0);
INSERT INTO experiments (simulation_id, path_id, trace_id, scheduler_name, name, state, last_simulated_tick)
VALUES (1, 1, 3, 'SRTF', 'Path planning trace, FIFI', 'QUEUED', 0);
INSERT INTO experiments (simulation_id, path_id, trace_id, scheduler_name, name, state, last_simulated_tick)
VALUES (1, 1, 1, 'FIFO', 'Default trace, SRTF', 'QUEUED', 0);
INSERT INTO experiments (simulation_id, path_id, trace_id, scheduler_name, name, state, last_simulated_tick)
VALUES (1, 1, 2, 'FIFO', 'Image processing trace, SRTF', 'QUEUED', 0);
INSERT INTO experiments (simulation_id, path_id, trace_id, scheduler_name, name, state, last_simulated_tick)
VALUES (1, 1, 3, 'FIFO', 'Path planning trace, FIFO', 'QUEUED', 0);

-- Rooms
INSERT INTO rooms (name, datacenter_id, type) VALUES ('room 1', 1, 'SERVER');
INSERT INTO rooms (name, datacenter_id, type) VALUES ('room 1', 2, 'SERVER');
INSERT INTO rooms (name, datacenter_id, type) VALUES ('room 1', 3, 'SERVER');
INSERT INTO rooms (name, datacenter_id, type) VALUES ('room 2', 3, 'SERVER');
INSERT INTO rooms (name, datacenter_id, type) VALUES ('Power Room', 1, 'POWER');

-- Tiles
INSERT INTO tiles (position_x, position_y, room_id) VALUES (10, 10, 1);
INSERT INTO tiles (position_x, position_y, room_id) VALUES (9, 10, 1);
INSERT INTO tiles (position_x, position_y, room_id) VALUES (10, 11, 1);

INSERT INTO tiles (position_x, position_y, room_id) VALUES (10, 10, 2);
INSERT INTO tiles (position_x, position_y, room_id) VALUES (9, 10, 2);
INSERT INTO tiles (position_x, position_y, room_id) VALUES (10, 11, 2);
INSERT INTO tiles (position_x, position_y, room_id) VALUES (11, 11, 2);

INSERT INTO tiles (position_x, position_y, room_id) VALUES (10, 10, 3);
INSERT INTO tiles (position_x, position_y, room_id) VALUES (9, 10, 3);
INSERT INTO tiles (position_x, position_y, room_id) VALUES (10, 11, 3);
INSERT INTO tiles (position_x, position_y, room_id) VALUES (11, 11, 3);
INSERT INTO tiles (position_x, position_y, room_id) VALUES (11, 10, 4);
INSERT INTO tiles (position_x, position_y, room_id) VALUES (12, 10, 4);

INSERT INTO tiles (position_x, position_y, room_id) VALUES (10, 12, 5);
INSERT INTO tiles (position_x, position_y, room_id) VALUES (10, 13, 5);

-- Racks
INSERT INTO objects (type) VALUES ('RACK');
INSERT INTO racks (capacity, name, power_capacity_w) VALUES (42, 'rack 1', 5000);
UPDATE tiles
SET object_id = 1
WHERE id = 1;
INSERT INTO objects (type) VALUES ('RACK');
INSERT INTO racks (capacity, name, power_capacity_w) VALUES (42, 'rack 2', 5000);
UPDATE tiles
SET object_id = 2
WHERE id = 2;

INSERT INTO objects (type) VALUES ('RACK');
INSERT INTO racks (capacity, name, power_capacity_w) VALUES (42, 'rack 1', 5000);
UPDATE tiles
SET object_id = 3
WHERE id = 4;
INSERT INTO objects (type) VALUES ('RACK');
INSERT INTO racks (capacity, name, power_capacity_w) VALUES (42, 'rack 2', 5000);
UPDATE tiles
SET object_id = 4
WHERE id = 5;
INSERT INTO objects (type) VALUES ('RACK');
INSERT INTO racks (capacity, name, power_capacity_w) VALUES (42, 'rack 3', 5000);
UPDATE tiles
SET object_id = 5
WHERE id = 7;

INSERT INTO objects (type) VALUES ('RACK');
INSERT INTO racks (capacity, name, power_capacity_w) VALUES (42, 'rack 1', 5000);
UPDATE tiles
SET object_id = 6
WHERE id = 8;

INSERT INTO objects (type) VALUES ('RACK');
INSERT INTO racks (capacity, name, power_capacity_w) VALUES (42, 'rack 2', 5000);
UPDATE tiles
SET object_id = 7
WHERE id = 9;

INSERT INTO objects (type) VALUES ('RACK');
INSERT INTO racks (capacity, name, power_capacity_w) VALUES (42, 'rack 3', 5000);
UPDATE tiles
SET object_id = 8
WHERE id = 11;

INSERT INTO objects (type) VALUES ('RACK');
INSERT INTO racks (capacity, name, power_capacity_w) VALUES (42, 'rack 4', 5000);
UPDATE tiles
SET object_id = 9
WHERE id = 12;

-- Machines
INSERT INTO machines (rack_id, position) VALUES (1, 1);
INSERT INTO machines (rack_id, position) VALUES (1, 2);
INSERT INTO machines (rack_id, position) VALUES (1, 6);
INSERT INTO machines (rack_id, position) VALUES (1, 10);
INSERT INTO machines (rack_id, position) VALUES (2, 1);
INSERT INTO machines (rack_id, position) VALUES (2, 2);

INSERT INTO machines (rack_id, position) VALUES (3, 1);
INSERT INTO machines (rack_id, position) VALUES (3, 2);
INSERT INTO machines (rack_id, position) VALUES (3, 6);
INSERT INTO machines (rack_id, position) VALUES (3, 10);
INSERT INTO machines (rack_id, position) VALUES (4, 1);
INSERT INTO machines (rack_id, position) VALUES (4, 2);
INSERT INTO machines (rack_id, position) VALUES (5, 1);
INSERT INTO machines (rack_id, position) VALUES (5, 2);
INSERT INTO machines (rack_id, position) VALUES (5, 3);

INSERT INTO machines (rack_id, position) VALUES (6, 1);
INSERT INTO machines (rack_id, position) VALUES (6, 2);
INSERT INTO machines (rack_id, position) VALUES (6, 6);
INSERT INTO machines (rack_id, position) VALUES (6, 10);
INSERT INTO machines (rack_id, position) VALUES (7, 1);
INSERT INTO machines (rack_id, position) VALUES (7, 2);
INSERT INTO machines (rack_id, position) VALUES (8, 1);
INSERT INTO machines (rack_id, position) VALUES (8, 2);
INSERT INTO machines (rack_id, position) VALUES (8, 3);
INSERT INTO machines (rack_id, position) VALUES (9, 4);
INSERT INTO machines (rack_id, position) VALUES (9, 5);
INSERT INTO machines (rack_id, position) VALUES (9, 6);
INSERT INTO machines (rack_id, position) VALUES (9, 7);

-- Tags
INSERT INTO machine_tags (name, machine_id) VALUES ('my fave machine', 1);
INSERT INTO machine_tags (name, machine_id) VALUES ('my best machine', 2);

-- Failure models
INSERT INTO failure_models (name, rate) VALUES ('test_model', 0);

-- CPUs
INSERT INTO cpus (manufacturer, family, generation, model, clock_rate_mhz, number_of_cores, energy_consumption_w,
                  failure_model_id) VALUES ('intel', 'i7', 'v6', '6700k', 4100, 4, 70, 1);
INSERT INTO cpus (manufacturer, family, generation, model, clock_rate_mhz, number_of_cores, energy_consumption_w,
                  failure_model_id) VALUES ('intel', 'i5', 'v6', '6700k', 3500, 2, 50, 1);

-- GPUs
INSERT INTO gpus (manufacturer, family, generation, model, clock_rate_mhz, number_of_cores, energy_consumption_w,
                  failure_model_id) VALUES ("NVIDIA", "GTX", "4", "1080", 1200, 200, 250, 1);

-- CPUs in machines
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (1, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (1, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (1, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (2, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (2, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (3, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (3, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (3, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (4, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (4, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (4, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (5, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (6, 1);

INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (7, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (7, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (7, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (8, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (8, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (9, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (9, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (9, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (10, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (10, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (10, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (11, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (12, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (13, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (14, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (15, 1);

INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (16, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (16, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (16, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (17, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (17, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (18, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (18, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (18, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (19, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (19, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (19, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (20, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (21, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (22, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (23, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (24, 1);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (25, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (26, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (27, 2);
INSERT INTO machine_cpus (machine_id, cpu_id) VALUES (28, 2);

-- GPUs
INSERT INTO gpus (manufacturer, family, generation, model, clock_rate_mhz, number_of_cores, energy_consumption_w,
                  failure_model_id) VALUES ('nvidia', 'GeForce GTX Series', '10', '80', 1607, 2560, 70, 1);

-- Memories

INSERT INTO memories (manufacturer, family, generation, model, speed_mb_per_s, size_mb, energy_consumption_w,
                      failure_model_id) VALUES ('samsung', 'PC DRAM', 'K4A4G045WD', 'DDR4', 16000, 4000, 10, 1);

-- Storages

INSERT INTO storages (manufacturer, family, generation, model, speed_mb_per_s, size_mb, energy_consumption_w,
                      failure_model_id) VALUES ('samsung', 'EVO', '2016', 'SATA III', 6000, 250000, 10, 1);

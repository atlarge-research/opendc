-- Tables referred to in foreign key constraints are defined after the constraints are defined
SET FOREIGN_KEY_CHECKS = 0;

/*
*   A user is identified by their google_id, which the server gets by authenticating with Google.
*/

-- Users
DROP TABLE IF EXISTS users;
CREATE TABLE users (
  id          INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  google_id   TEXT                    NOT NULL,
  email       TEXT,
  given_name  TEXT,
  family_name TEXT
);

/*
*   The authorizations table defines which users are authorized to "OWN", "EDIT", or "VIEW" a simulation. The
*   authorization_level table defines the permission levels.
*/

-- User authorizations
DROP TABLE IF EXISTS authorizations;
CREATE TABLE authorizations (
  user_id             INTEGER     NOT NULL,
  simulation_id       INTEGER     NOT NULL,
  authorization_level VARCHAR(50) NOT NULL,

  FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (simulation_id) REFERENCES simulations (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (authorization_level) REFERENCES authorization_levels (level)
);

CREATE UNIQUE INDEX authorizations_index
  ON authorizations (
    user_id,
    simulation_id
  );

-- Authorization levels
DROP TABLE IF EXISTS authorization_levels;
CREATE TABLE authorization_levels (
  level VARCHAR(50) PRIMARY KEY        NOT NULL
);
INSERT INTO authorization_levels (level) VALUES ('OWN');
INSERT INTO authorization_levels (level) VALUES ('EDIT');
INSERT INTO authorization_levels (level) VALUES ('VIEW');

/*
*   A Simulation has several Paths, which define the topology of the datacenter at different times. A Simulation also
*   has several Experiments, which can be run on a combination of Paths, Schedulers and Traces. Simulations also serve
*   as the scope to which different Users can be Authorized.
*
*   The datetime_created and datetime_last_edited columns are in a subset of ISO-8601 (second fractions are ommitted): 
*   YYYY-MM-DDTHH:MM:SS, where...
*       -   YYYY is the four-digit year,
*       -   MM is the two-digit month (1-12)
*       -   DD is the two-digit day of the month (1-31)
*       -   HH is the two-digit hours part (0-23)
*       -   MM is the two-digit minutes part (0-59)
*       -   SS is the two-digit secodns part (0-59)
*/

-- Simulation
DROP TABLE IF EXISTS simulations;
CREATE TABLE simulations (
  id                   INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  datetime_created     VARCHAR(50)             NOT NULL CHECK (datetime_created LIKE '____-__-__T__:__:__'),
  datetime_last_edited VARCHAR(50)             NOT NULL CHECK (datetime_last_edited LIKE '____-__-__T__:__:__'),
  name                 VARCHAR(50)             NOT NULL
);

/*
*   An Experiment consists of a Path, a Scheduler, and a Trace. The Path defines the topology of the datacenter at
*   different times in the simulation. The Scheduler defines which scheduler to use to simulate this experiment. The
*   Trace defines which tasks have to be run in the simulation.
*/

DROP TABLE IF EXISTS experiments;
CREATE TABLE experiments (
  id                  INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  simulation_id       INTEGER                 NOT NULL,
  path_id             INTEGER                 NOT NULL,
  trace_id            INTEGER                 NOT NULL,
  scheduler_name      VARCHAR(50)             NOT NULL,
  name                TEXT                    NOT NULL,
  state               TEXT                    NOT NULL,
  last_simulated_tick INTEGER                 NOT NULL DEFAULT 0 CHECK (last_simulated_tick >= 0),

  FOREIGN KEY (simulation_id) REFERENCES simulations (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (path_id) REFERENCES paths (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (trace_id) REFERENCES traces (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (scheduler_name) REFERENCES schedulers (name)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

/*
*   A Simulation has several Paths, which each contain Sections. A Section details which Datacenter topology to use
*   starting at which point in time (known internally as a "tick"). So, combining the several Sections in a Path
*   tells us which Datacenter topology to use at each tick.
*/

-- Path
DROP TABLE IF EXISTS paths;
CREATE TABLE paths (
  id               INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  simulation_id    INTEGER                 NOT NULL,
  name             TEXT,
  datetime_created VARCHAR(50)             NOT NULL CHECK (datetime_created LIKE '____-__-__T__:__:__'),

  FOREIGN KEY (simulation_id) REFERENCES simulations (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Sections
DROP TABLE IF EXISTS sections;
CREATE TABLE sections (
  id            INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  path_id       INTEGER                 NOT NULL,
  datacenter_id INTEGER                 NOT NULL,
  start_tick    INTEGER                 NOT NULL CHECK (start_tick >= 0),

  FOREIGN KEY (path_id) REFERENCES paths (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (datacenter_id) REFERENCES datacenters (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Scheduler names
DROP TABLE IF EXISTS schedulers;
CREATE TABLE schedulers (
  name VARCHAR(50) PRIMARY KEY        NOT NULL
);
INSERT INTO schedulers (name) VALUES ('DEFAULT');
INSERT INTO schedulers (name) VALUES ('SRTF');
INSERT INTO schedulers (name) VALUES ('FIFO');

/*
*   Each simulation has a single trace. A trace contains tasks and their start times.
*/

-- A trace describes when tasks arrives in a datacenter
DROP TABLE IF EXISTS traces;
CREATE TABLE traces (
  id   INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  name TEXT                    NOT NULL
);

-- A job
DROP TABLE IF EXISTS jobs;
CREATE TABLE jobs (
  id       INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  name     TEXT                    NOT NULL,
  trace_id INTEGER                 NOT NULL,

  FOREIGN KEY (trace_id) REFERENCES traces (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- A task that's defined in terms of how many flops (floating point operations) it takes to complete
DROP TABLE IF EXISTS tasks;
CREATE TABLE tasks (
  id                 INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  start_tick         INTEGER                 NOT NULL CHECK (start_tick >= 0),
  total_flop_count   INTEGER                 NOT NULL,
  job_id             INTEGER                 NOT NULL,
  task_dependency_id INTEGER                 NULL,
  parallelizability  VARCHAR(50)             NOT NULL,

  FOREIGN KEY (job_id) REFERENCES jobs (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (task_dependency_id) REFERENCES tasks (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

/*
*   A task_state describes how much of a task has already been completed at the time of the current tick. Several
*   machine_states show which machines worked on the task.
*/

-- A state for a task_flop
DROP TABLE IF EXISTS task_states;
CREATE TABLE task_states (
  id            INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  task_id       INTEGER                 NOT NULL,
  experiment_id INTEGER                 NOT NULL,
  tick          INTEGER                 NOT NULL CHECK (tick >= 0),
  flops_left    INTEGER                 NOT NULL CHECK (flops_left >= 0),
  cores_used    INTEGER                 NOT NULL CHECK (cores_used >= 0),

  FOREIGN KEY (task_id) REFERENCES tasks (id),
  FOREIGN KEY (experiment_id) REFERENCES experiments (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- A machine state
DROP TABLE IF EXISTS machine_states;
CREATE TABLE machine_states (
  id               INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  task_id          INTEGER,
  machine_id       INTEGER                 NOT NULL,
  experiment_id    INTEGER                 NOT NULL,
  tick             INTEGER                 NOT NULL,
  temperature_c    REAL,
  in_use_memory_mb INTEGER,
  load_fraction    REAL CHECK (load_fraction >= 0 AND load_fraction <= 1),

  FOREIGN KEY (task_id) REFERENCES tasks (id),
  FOREIGN KEY (machine_id) REFERENCES machines (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (experiment_id) REFERENCES experiments (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

/*
*   A Section references a Datacenter topology, which can be used by multiple Sections to create Paths that go back and
*   forth between different topologies.
*/

-- Datacenters
DROP TABLE IF EXISTS datacenters;
CREATE TABLE datacenters (
  id            INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  simulation_id INTEGER                 NOT NULL,
  starred       INTEGER CHECK (starred = 0 OR starred = 1),

  FOREIGN KEY (simulation_id) REFERENCES simulations (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

/*
*   A datacenter consists of several rooms. A room has a type that specifies what kind of objects can be in it.
*/

-- Rooms in a datacenter
DROP TABLE IF EXISTS rooms;
CREATE TABLE rooms (
  id            INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  name          TEXT                    NOT NULL,
  datacenter_id INTEGER                 NOT NULL,
  type          VARCHAR(50)             NOT NULL,
  topology_id   INTEGER,

  FOREIGN KEY (datacenter_id) REFERENCES datacenters (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (type) REFERENCES room_types (name)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (topology_id) REFERENCES rooms (id)
    ON DELETE NO ACTION
    ON UPDATE CASCADE
);

DROP TABLE IF EXISTS room_types;
CREATE TABLE room_types (
  name VARCHAR(50) PRIMARY KEY        NOT NULL
);
INSERT INTO room_types (name) VALUES ('SERVER');
INSERT INTO room_types (name) VALUES ('HALLWAY');
INSERT INTO room_types (name) VALUES ('OFFICE');
INSERT INTO room_types (name) VALUES ('POWER');
INSERT INTO room_types (name) VALUES ('COOLING');

/*
*   A room consists of tiles that have a quantized (x,y) position. The same tile can't be in multiple rooms. All tiles 
*   in a room must touch at least one edge to another tile in that room. A tile is occupied by a single object, which
*   has a type from the object_types table.
*/

-- Tiles in a room
DROP TABLE IF EXISTS tiles;
CREATE TABLE tiles (
  id          INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  position_x  INTEGER                 NOT NULL,
  position_y  INTEGER                 NOT NULL,
  room_id     INTEGER                 NOT NULL,
  object_id   INTEGER,
  topology_id INTEGER,

  FOREIGN KEY (room_id) REFERENCES rooms (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (object_id) REFERENCES objects (id),
  FOREIGN KEY (topology_id) REFERENCES tiles (id)
    ON DELETE NO ACTION
    ON UPDATE CASCADE,

  UNIQUE (position_x, position_y, room_id), -- only one tile can be in the same position in a room
  UNIQUE (object_id)                        -- an object can only be on one tile
);

DELIMITER //

-- Make sure this datacenter doesn't already have a tile in this location
-- and tiles in a room are connected.
DROP TRIGGER IF EXISTS before_insert_tiles_check_existence;
CREATE TRIGGER before_insert_tiles_check_existence
BEFORE INSERT ON tiles
FOR EACH ROW
  BEGIN
    -- checking tile overlap
    -- a tile already exists such that..
    IF EXISTS(SELECT datacenter_id
              FROM tiles
                JOIN rooms ON tiles.room_id = rooms.id
              WHERE (

                -- it's in the same datacenter as the new tile...
                datacenter_id = (SELECT datacenter_id
                                 FROM rooms
                                 WHERE rooms.id = NEW.room_id)

                -- and in the the same position as the new tile.
                AND NEW.position_x = tiles.position_x AND NEW.position_y = tiles.position_y
              ))
    THEN
      -- raise an error
      SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'OccupiedTilePosition';
    END IF;

    -- checking tile adjacency
    -- this isn't the first tile, ...
    IF (EXISTS(SELECT *
               FROM tiles
               WHERE (NEW.room_id = tiles.room_id))

        -- and the new tile isn't directly to right, to the left, above, or below an exisiting tile.
        AND NOT EXISTS(SELECT *
                       FROM tiles
                       WHERE (
                         NEW.room_id = tiles.room_id AND (
                           (NEW.position_x + 1 = tiles.position_x AND NEW.position_y = tiles.position_y)     -- right
                           OR (NEW.position_x - 1 = tiles.position_x AND NEW.position_y = tiles.position_y)  -- left
                           OR (NEW.position_x = tiles.position_x AND NEW.position_y + 1 = tiles.position_y)  -- above
                           OR (NEW.position_x = tiles.position_x AND NEW.position_y - 1 = tiles.position_y)  -- below
                         )
                       )))
    THEN
      -- raise an error
      SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'InvalidTilePosition';
    END IF;
  END//

DELIMITER ;

/*
*   Objects are on tiles and have a type. They form an extra abstraction layer to make it easier to find what object is
*   on a tile, as well as to enforce that only objects of the right type are in a certain room.
*
*   To add a PSU, cooling item, or rack to a tile, first add an object. Then use that object's ID as the value for the
*   object_id column of the PSU, cooling item, or rack table.
*   
*   The allowed_object table specifies what types of objects are allowed in what types of rooms.
*/

-- Objects
DROP TABLE IF EXISTS objects;
CREATE TABLE objects (
  id   INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  type VARCHAR(50)             NOT NULL,

  FOREIGN KEY (type) REFERENCES object_types (name)
);

-- Object types
DROP TABLE IF EXISTS object_types;
CREATE TABLE object_types (
  name VARCHAR(50) PRIMARY KEY        NOT NULL
);
INSERT INTO object_types (name) VALUES ('PSU');
INSERT INTO object_types (name) VALUES ('COOLING_ITEM');
INSERT INTO object_types (name) VALUES ('RACK');

-- Allowed objects table
DROP TABLE IF EXISTS allowed_objects;
CREATE TABLE allowed_objects (
  room_type   VARCHAR(50) NOT NULL,
  object_type VARCHAR(50) NOT NULL,

  FOREIGN KEY (room_type) REFERENCES room_types (name),
  FOREIGN KEY (object_type) REFERENCES object_types (name)
);

-- Allowed objects per room
INSERT INTO allowed_objects (room_type, object_type) VALUES ('SERVER', 'RACK');
-- INSERT INTO allowed_objects (room_type, object_type) VALUES ('POWER', 'PSU');
-- INSERT INTO allowed_objects (room_type, object_type) VALUES ('COOLING', 'COOLING_ITEM');

DELIMITER //

-- Make sure objects are added to tiles in rooms they're allowed to be in.
DROP TRIGGER IF EXISTS before_update_tiles;
CREATE TRIGGER before_update_tiles
BEFORE UPDATE ON tiles
FOR EACH ROW
  BEGIN

    IF ((NEW.object_id IS NOT NULL) AND (

      -- the type of the object being added to the tile...
      (
        SELECT objects.type
        FROM objects
          JOIN tiles ON tiles.object_id = objects.id
        WHERE tiles.id = NEW.id
      )

      -- is not in the set of allowed object types for the room the tile is in.
      NOT IN (
        SELECT object_type
        FROM allowed_objects
          JOIN rooms ON rooms.type = allowed_objects.room_type
        WHERE rooms.id = NEW.room_id
      )
    ))
    THEN
      -- raise an error
      SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'ForbiddenObjectType';
    END IF;
  END//

DELIMITER ;

/*
*   PSUs are a type of object.
*/

-- PSUs on tiles
DROP TABLE IF EXISTS psus;
CREATE TABLE psus (
  id               INTEGER     NOT NULL,
  energy_kwh       INTEGER     NOT NULL CHECK (energy_kwh > 0),
  type             VARCHAR(50) NOT NULL,
  failure_model_id INTEGER     NOT NULL,

  FOREIGN KEY (id) REFERENCES objects (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (failure_model_id) REFERENCES failure_models (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  PRIMARY KEY (id)
);

/*
*   Cooling items are a type of object.
*/

-- Cooling items on tiles
DROP TABLE IF EXISTS cooling_items;
CREATE TABLE cooling_items (
  id                   INTEGER     NOT NULL,
  energy_consumption_w INTEGER     NOT NULL CHECK (energy_consumption_w > 0),
  type                 VARCHAR(50) NOT NULL,
  failure_model_id     INTEGER     NOT NULL,

  FOREIGN KEY (id) REFERENCES objects (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (failure_model_id) REFERENCES failure_models (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  PRIMARY KEY (id)
);

/*
*   Racks are a type of object.
*/

-- Racks on tiles
DROP TABLE IF EXISTS racks;
CREATE TABLE racks (
  id               INTEGER NOT NULL,
  name             TEXT,
  capacity         INTEGER NOT NULL CHECK (capacity > 0),
  power_capacity_w INTEGER NOT NULL CHECK (power_capacity_w > 0),
  topology_id      INTEGER,

  FOREIGN KEY (id) REFERENCES objects (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (topology_id) REFERENCES racks (id)
    ON DELETE NO ACTION
    ON UPDATE CASCADE,

  PRIMARY KEY (id)
);

/*
*   A rack contains a number of machines. A rack cannot have more than its capacity of machines in it. No more than one
*   machine can occupy a position in a rack at the same time.
*/

-- Machines in racks
DROP TABLE IF EXISTS machines;
CREATE TABLE machines (
  id          INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  rack_id     INTEGER                 NOT NULL,
  position    INTEGER                 NOT NULL CHECK (position > 0),
  topology_id INTEGER,

  FOREIGN KEY (rack_id) REFERENCES racks (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (topology_id) REFERENCES machines (id)
    ON DELETE NO ACTION
    ON UPDATE CASCADE,

  -- Prevent machines from occupying the same position in a rack.
  UNIQUE (rack_id, position)
);

DELIMITER //

-- Make sure a machine is not inserted at a position that does not exist for its rack.
DROP TRIGGER IF EXISTS before_insert_machine;
CREATE TRIGGER before_insert_machine
BEFORE INSERT ON machines
FOR EACH ROW
  BEGIN
    IF (
      NEW.position > (SELECT capacity
                      FROM racks
                      WHERE racks.id = NEW.rack_id)
    )
    THEN
      -- raise an error
      SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'InvalidMachinePosition';
    END IF;
  END//

DELIMITER ;

/*
*   A machine can have a tag for easy search and filtering.
*/

-- Tags for machines
DROP TABLE IF EXISTS machine_tags;
CREATE TABLE machine_tags (
  name       TEXT    NOT NULL,
  machine_id INTEGER NOT NULL,

  FOREIGN KEY (machine_id) REFERENCES machines (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

/*
*   A failure model defines the probability of a machine breaking at any given time.
*/

-- Failure models
DROP TABLE IF EXISTS failure_models;
CREATE TABLE failure_models (
  id   INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  name TEXT                    NOT NULL,
  rate REAL                    NOT NULL CHECK (rate >= 0 AND rate <= 1)
);

/*
*   A cpu stores information about a type of cpu. The machine_cpu table keeps track of which cpus are in which machines.
*/

-- CPU specs
DROP TABLE IF EXISTS cpus;
CREATE TABLE cpus (
  id                   INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  manufacturer         TEXT                    NOT NULL,
  family               TEXT                    NOT NULL,
  generation           TEXT                    NOT NULL,
  model                TEXT                    NOT NULL,
  clock_rate_mhz       INTEGER                 NOT NULL CHECK (clock_rate_mhz > 0),
  number_of_cores      INTEGER                 NOT NULL CHECK (number_of_cores > 0),
  energy_consumption_w REAL                    NOT NULL CHECK (energy_consumption_w > 0),
  failure_model_id     INTEGER                 NOT NULL,

  FOREIGN KEY (failure_model_id) REFERENCES failure_models (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- CPUs in machines
DROP TABLE IF EXISTS machine_cpus;
CREATE TABLE machine_cpus (
  id         INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  machine_id INTEGER                 NOT NULL,
  cpu_id     INTEGER                 NOT NULL,

  FOREIGN KEY (machine_id) REFERENCES machines (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (cpu_id) REFERENCES cpus (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

/*
*   A gpu stores information about a type of gpu. The machine_gpu table keeps track of which gpus are in which machines.
*/

-- GPU specs
DROP TABLE IF EXISTS gpus;
CREATE TABLE gpus (
  id                   INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  manufacturer         TEXT                    NOT NULL,
  family               TEXT                    NOT NULL,
  generation           TEXT                    NOT NULL,
  model                TEXT                    NOT NULL,
  clock_rate_mhz       INTEGER                 NOT NULL CHECK (clock_rate_mhz > 0),
  number_of_cores      INTEGER                 NOT NULL CHECK (number_of_cores > 0),
  energy_consumption_w REAL                    NOT NULL CHECK (energy_consumption_w > 0),
  failure_model_id     INTEGER                 NOT NULL,

  FOREIGN KEY (failure_model_id) REFERENCES failure_models (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- GPUs in machines
DROP TABLE IF EXISTS machine_gpus;
CREATE TABLE machine_gpus (
  id         INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  machine_id INTEGER                 NOT NULL,
  gpu_id     INTEGER                 NOT NULL,

  FOREIGN KEY (machine_id) REFERENCES machines (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (gpu_id) REFERENCES gpus (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

/*
*   A memory stores information about a type of memory. The machine_memory table keeps track of which memories are in 
*   which machines.
*/

-- Memory specs
DROP TABLE IF EXISTS memories;
CREATE TABLE memories (
  id                   INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  manufacturer         TEXT                    NOT NULL,
  family               TEXT                    NOT NULL,
  generation           TEXT                    NOT NULL,
  model                TEXT                    NOT NULL,
  speed_mb_per_s       INTEGER                 NOT NULL CHECK (speed_mb_per_s > 0),
  size_mb              INTEGER                 NOT NULL CHECK (size_mb > 0),
  energy_consumption_w REAL                    NOT NULL CHECK (energy_consumption_w > 0),
  failure_model_id     INTEGER                 NOT NULL,

  FOREIGN KEY (failure_model_id) REFERENCES failure_models (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Memory in machines
DROP TABLE IF EXISTS machine_memories;
CREATE TABLE machine_memories (
  id         INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  machine_id INTEGER                 NOT NULL,
  memory_id  INTEGER                 NOT NULL,

  FOREIGN KEY (machine_id) REFERENCES machines (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (memory_id) REFERENCES memories (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

/*
*   A storage stores information about a type of storage. The machine_storage table keeps track of which storages are in 
*   which machines.
*/

-- Storage specs
DROP TABLE IF EXISTS storages;
CREATE TABLE storages (
  id                   INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  manufacturer         TEXT                    NOT NULL,
  family               TEXT                    NOT NULL,
  generation           TEXT                    NOT NULL,
  model                TEXT                    NOT NULL,
  speed_mb_per_s       INTEGER                 NOT NULL CHECK (speed_mb_per_s > 0),
  size_mb              INTEGER                 NOT NULL CHECK (size_mb > 0),
  energy_consumption_w REAL                    NOT NULL CHECK (energy_consumption_w > 0),
  failure_model_id     INTEGER                 NOT NULL,

  FOREIGN KEY (failure_model_id) REFERENCES failure_models (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Storage in machines
DROP TABLE IF EXISTS machine_storages;
CREATE TABLE machine_storages (
  id         INTEGER PRIMARY KEY     NOT NULL AUTO_INCREMENT,
  machine_id INTEGER                 NOT NULL,
  storage_id INTEGER                 NOT NULL,

  FOREIGN KEY (machine_id) REFERENCES machines (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  FOREIGN KEY (storage_id) REFERENCES storages (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

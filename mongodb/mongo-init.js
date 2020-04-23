db.auth('root', 'rootpassword')

let error = true

db.createUser(
  {
      user: "admin",
      pwd: "adminpassword",
      roles: [
          {
              role: "readWrite",
              db: "admin"
          }
      ]
  }
)

db = db.getSiblingDB('opendc')

db.createUser(
        {
            user: "opendc",
            pwd: "opendcpassword",
            roles: [
                {
                    role: "readWrite",
                    db: "opendc"
                }
            ]
        }
);

db.createCollection(users)
db.createCollection(authorizations)
db.createCollection(authorization_levels)
db.createCollection(simulations)
db.createCollection(experiments)
db.createCollection(paths)
db.createCollection(sections)
db.createCollection(schedulers)
db.createCollection(traces)
db.createCollection(jobs)
db.createCollection(tasks)
db.createCollection(task_dependencies)
db.createCollection(task_states)
db.createCollection(machine_states)
db.createCollection(datacenters)
db.createCollection(rooms)
db.createCollection(room_types)
db.createCollection(tiles)
db.createCollection(objects)
db.createCollection(object_types)
db.createCollection(allowed_objects)
db.createCollection(psus)
db.createCollection(cooling_items)
db.createCollection(racks)
db.createCollection(machines)
db.createCollection(machine_tags)
db.createCollection(failure_models)
db.createCollection(cpus)
db.createCollection(machine_cpus)
db.createCollection(gpus)
db.createCollection(machine_gpus)
db.createCollection(memories)
db.createCollection(machine_memories)
db.createCollection(storages)
db.createCollection(machine_storages)
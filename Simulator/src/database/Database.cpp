#include "database/Database.h"
#include "database/Queries.h"
#include "database/QueryExecuter.h"

#include "modeling/ModelingTypes.h"
#include "modeling/machine/CPU.h"

#include "simulation/schedulers/ShortestRemainingTimeScheduler.h"

#include <sqlite3.h>
#include <assert.h>
#include "simulation/Experiment.h"
#include "simulation/schedulers/FirstInFirstOutScheduler.h"

namespace Database
{
	Database::Database(char* name)
	{
		int rc = sqlite3_open_v2(name, &db, SQLITE_OPEN_READWRITE, NULL);
		assert(rc == SQLITE_OK);
	}

	Database::~Database()
	{
		int rc = sqlite3_close_v2(db);
		assert(rc == SQLITE_OK);
	}

	void Database::startTransaction() const
	{
		sqlite3_exec(db, "BEGIN TRANSACTION;", NULL, NULL, NULL);
	}

	void Database::endTransaction() const
	{
		sqlite3_exec(db, "END TRANSACTION;", NULL, NULL, NULL);
	}

	void Database::writeExperimentHistory(Simulation::Experiment& experiment) const
	{
		auto history = experiment.getHistory();

		auto workloadHistory = std::get<0>(history.getHistory());

		QueryExecuter<> writeWorkloadStateQuery(db);
		writeWorkloadStateQuery.setQuery(Queries::WRITE_WORKLOAD_STATE);
		std::for_each(workloadHistory.get().begin(), workloadHistory.get().end(), [&](const auto& pair) {
			uint32_t tick = pair.first;
			Simulation::WorkloadSnapshot snapshot = pair.second;

			uint32_t id = snapshot.id;
			uint32_t flopsDone = snapshot.flopsDone;
			uint32_t coresUsed = snapshot.coresUsed;
			writeWorkloadStateQuery.reset()
				.bindParams<int, int, int, int, int>(id, experiment.getId(), tick, flopsDone, coresUsed)
				.executeOnce();
		});

		auto machineHistory = std::get<1>(history.getHistory());

		QueryExecuter<> writeMachineStateQuery(db);
		writeMachineStateQuery.setQuery(Queries::WRITE_MACHINE_STATE);

		std::for_each(machineHistory.get().begin(), machineHistory.get().end(), [&](const auto& pair) {
			uint32_t tick = pair.first;
			Simulation::MachineSnapshot snapshot = pair.second;

			uint32_t id = snapshot.id;
			uint32_t workloadId = snapshot.currentWorkload;
			float temp = snapshot.temperature;
			float load = snapshot.loadFraction;

			uint32_t mem = snapshot.usedMemory;
			writeMachineStateQuery.reset()
				.bindParams<int, int, int, int, float, int, float>(workloadId, id, experiment.getId(), tick, temp, mem, load)
				.executeOnce();
		});

		history.clearHistory();

		uint32_t lastSimulatedTick = experiment.getCurrentTick() != 0 ? experiment.getCurrentTick() - 1 : 0;
		QueryExecuter<> writeLastSimulatedTick(db);
		writeLastSimulatedTick.setQuery(Queries::WRITE_EXPERIMENT_LAST_SIMULATED_TICK)
			.bindParams<int, int>(lastSimulatedTick, experiment.getId())
			.executeOnce();
	}

	int Database::pollQueuedExperiments() const
	{
		QueryExecuter<int> q(db);
		q.setQuery(Queries::GET_QUEUED_EXPERIMENTS);
		bool hasRow = q.step();
		if(hasRow)
			return q.result().get<int, 0>();
		return -1;
	}

	void Database::dequeueExperiment(int experimentId) const
	{
		QueryExecuter<> q(db);
		q.setQuery(Queries::SET_EXPERIMENT_STATE_SIMULATING)
			.bindParams<int>(experimentId)
			.executeOnce();
	}

	void Database::finishExperiment(int id) const
	{
		QueryExecuter<> q(db);
		q.setQuery(Queries::SET_EXPERIMENT_STATE_FINISHED)
			.bindParams<int>(id)
			.executeOnce();
	}


	Simulation::Experiment Database::createExperiment(uint32_t experimentId)
	{
		// Retrieves the experiment data by ID 
		QueryExecuter<int, int, int, int, std::string, std::string> q(db);
		QueryResult<int, int, int, int, std::string, std::string> qres = q
			.setQuery(Queries::GET_EXPERIMENT_BY_ID)
			.bindParams<int>(experimentId)
			.executeOnce();

		// Sets the scheduler of the datacenter
		Simulation::Scheduler* scheduler = loadScheduler(experimentId);

		int pathId = qres.get<int, 2>();
		Simulation::Path path = Simulation::Path(pathId);

		QueryExecuter<int, int, int, int> q2(db);
		std::vector<QueryResult<int, int, int, int>> q2res = q2
			.setQuery(Queries::GET_SECTION_BY_PATH_ID)
			.bindParams<int>(pathId)
			.execute();

		// Retrieve workloads of trace
		Simulation::WorkloadPool pool = loadWorkloads(experimentId);

		std::for_each(q2res.begin(), q2res.end(), [&](QueryResult<int, int, int, int> r) {
			int datacenterId = r.get<int, 2>();
			int startTick = r.get<int, 3>();
			DefaultDatacenter datacenter = loadDatacenter(datacenterId);
			DefaultSection section(datacenter, startTick);

			path.addSection(section);
		});

		Simulation::Experiment experiment(path, scheduler, pool, experimentId);

		return experiment;
	}

	Simulation::Scheduler* Database::loadScheduler(uint32_t experimentId) const
	{
		std::string name = QueryExecuter<std::string>(db)
			.setQuery(Queries::GET_SCHEDULER_TYPE_OF_EXPERIMENT)
			.bindParams<int>(experimentId)
			.executeOnce()
			.get<std::string, 0>();

		// Retrieve scheduler
		Simulation::Scheduler* scheduler = nullptr;
		if(name == "DEFAULT")
			scheduler = new Simulation::FirstInFirstOutScheduler();
		else if(name == "SRTF") // Shortest remaining time first
			scheduler = new Simulation::ShortestRemainingTimeScheduler();
		else if(name == "FIFO")
			scheduler = new Simulation::FirstInFirstOutScheduler();

		assert(scheduler != nullptr);
		return scheduler;
	}

	DefaultDatacenter Database::loadDatacenter(uint32_t datacenterId) const
	{
		DefaultDatacenter datacenter;

		// Retrieves a vector of rooms of the datacenter
		std::vector<QueryResult<int, std::string, int, std::string>> rooms = QueryExecuter<int, std::string, int, std::string>(db)
			.setQuery(Queries::GET_ROOMS_OF_DATACENTER)
			.bindParams<int>(datacenterId)
			.execute();

		// Get machines of rooms
		for(auto& room : rooms)
		{
			int id = room.get<int, 0>();
			Modeling::ServerRoom serverRoom(id);

			// Retrieves the racks in the room
			auto racks = QueryExecuter<int, std::string, int>(db)
				.setQuery(Queries::GET_RACKS_OF_ROOM)
				.bindParams<int>(id)
				.execute();

			for(auto& queryResult : racks)
			{
				int rackId = queryResult.get<int, 0>();

				// Retrieves the machines in the rack
				auto machinesResult = QueryExecuter<int, int>(db)
					.setQuery(Queries::GET_MACHINES_OF_RACK)
					.bindParams<int>(rackId)
					.execute();

				std::unordered_map<uint32_t, Modeling::Machine> machines;
				for(auto& qr : machinesResult)
				{
					int position = qr.get<int, 1>();
					int machineId = qr.get<int, 0>();
					machines.emplace(position, Modeling::Machine(machineId));
				}
				
				Modeling::Rack rack(rackId, machines);

				// Retrieves the cpus in the rack
				auto cpus = QueryExecuter<int, int, int, int, int>(db)
					.setQuery(Queries::GET_CPUS_IN_RACK)
					.bindParams<int>(rackId)
					.execute();

				for(auto& cpu : cpus)
				{
					int slot = cpu.get<int, 0>();
					int speed = cpu.get<int, 1>();
					int cores = cpu.get<int, 2>();
					int energyConsumption = cpu.get<int, 3>();
					int failureModelId = cpu.get<int, 4>();

					rack.getMachineAtSlot(slot).addCPU(Modeling::CPU(speed, cores, energyConsumption, failureModelId));
				}

				// Retrieves the gpus in the rack
				auto gpus = QueryExecuter<int, int, int, int, int>(db)
					.setQuery(Queries::GET_GPUS_IN_RACK)
					.bindParams<int>(rackId)
					.execute();

				for(auto& gpu : gpus)
				{
					int machineSlot = gpu.get<int, 0>();
					int speed = gpu.get<int, 1>();
					int cores = gpu.get<int, 2>();
					int energyConsumption = gpu.get<int, 3>();
					int failureModelId = gpu.get<int, 4>();

					rack.getMachineAtSlot(machineSlot).addGPU(Modeling::GPU(speed, cores, energyConsumption, failureModelId));
				}

				serverRoom.addEntity<Modeling::Rack>(rack);
			}

			datacenter.addRoomOfType<Modeling::ServerRoom>(serverRoom);
		}

		return datacenter;
	}

	Simulation::WorkloadPool Database::loadWorkloads(uint32_t simulationSectionId) const
	{
		Simulation::WorkloadPool pool;

		std::vector<QueryResult<int, int, int, int, int, std::string>> tasks;
		// Fetch tasks from database
		{
			// Retrieves the traceId corresponding to the simulation section
			QueryExecuter<int> q(db);
			int traceId = q
				.setQuery(Queries::GET_TRACE_OF_EXPERIMENT)
				.bindParams<int>(simulationSectionId)
				.executeOnce()
				.get<int, 0>();

			// Retrieves the tasks that belong to the traceId
			QueryExecuter<int, int, int, int, int, std::string> q2(db);
			tasks = q2
				.setQuery(Queries::GET_TASKS_OF_TRACE)
				.bindParams<int>(traceId)
				.execute();
		}

		// Create workloads from tasks
		for(auto& row : tasks)
		{
			int id = row.get<int, 0>();
			int startTick = row.get<int, 1>();
			int totalFlopCount = row.get<int, 2>();
			int traceId = row.get<int, 3>();
			int dependency = row.get<int, 4>();
			std::string parallelizability = row.get<std::string, 5>();
			bool parallel = false;
			if (parallelizability == "PARALLEL")
			{
				parallel = true;
			}

			// TODO possibly wait and batch?
			Simulation::Workload workload(totalFlopCount, startTick, id, traceId, dependency, parallel);
			if(dependency == 0)
				workload.dependencyFinished = true;

			pool.addWorkload(workload);
		}

		return pool;
	}
}


#pragma once
#include "simulation/Section.h"
#include "database/Database.h"

#include <iostream>
#include <unordered_map>

namespace Simulation
{
	/*
		The Simulator class controls (the creation of) all experiments, and providing access to the database.
	*/
	template<typename SimulationType>
	class Simulator
	{
	public:
		/*
			Initializes the simulator with an empty list of experiments, and with a reference to the database.
		*/
		explicit Simulator(char* databaseName) : database(databaseName) {}

		/*
			Adds a simulation to the list of experiments from the database, removing it from the queue
			of experiments in the database.
		*/
		void load(int experimentId)
		{
			Experiment experiment = database.createExperiment(experimentId);
			experiments.insert(std::make_pair(experimentId, experiment));
			database.dequeueExperiment(experimentId);
		}

		/*
			Polls the database for new jobs and simulates every queued simulation it finds.
		*/
		void pollAndLoadAll()
		{
			int rc = pollAndLoad();
			if (rc != -1)
				pollAndLoadAll();
		}

		/*
			Polls the database for new jobs and simulates the first it finds.
		*/
		int pollAndLoad()
		{
			int id = database.pollQueuedExperiments();
			if (id != -1)
			{
				std::cout << "Loaded simulation section " << id << std::endl;
				load(id);
			}
			return id;
		}

		/*
			Writes the state of all experiments if their history is size 3000 or larger.
		*/
		void writeHistoryAll()
		{
			if (experiments.size() == 0)
				return;

			auto it = experiments.begin();
			while(it != experiments.end())
			{
				auto history = (*it).second.getHistory();
				if (history.historySize() > 3000 || (*it).second.isFinished())
					write((*it).first);

				if ((*it).second.isFinished())
				{
					std::cout << "Finished simulation." << std::endl;
					database.finishExperiment((*it).first);
					it = experiments.erase(it);
				}
				else
				{
					++it;
				}
			}
		}

		/*
			Writes the state of the given simulation to the database.
		*/
		void write(int id)
		{
			std::cout << "Writing batch." << std::endl;
			database.startTransaction();
			database.writeExperimentHistory(experiments.at(id));
			database.endTransaction();
			auto history = experiments.at(id).getHistory();
			history.clearHistory();
			std::cout << "Finished writing batch." << std::endl;
		}
		
		/*
			Ticks each simulation once.
		*/
		void tickAll()
		{
			for (std::pair<const int, Experiment>& s : experiments)
				s.second.tick();
		}

		/*
			Ticks the given simulation once.
		*/
		void tick(int simulationId)
		{
			experiments.at(simulationId).tick();
		}

		/*
			Returns true if all experiments are finished.
		*/
		bool hasSimulations() const
		{
			return experiments.size() != 0;
		}

		/*
			Saves the state of all workloads to the history.
		*/
		void saveStateAll()
		{
			for (auto& pair : experiments)
				pair.second.saveState();
		}

	private:
		// The database to write results to.
		Database::Database database;

		// The list of experiments.
		std::unordered_map<int, Experiment> experiments;
	};
}

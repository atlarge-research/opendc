#pragma once
#include "Path.h"
#include <algorithm>
#include "workloads/WorkloadPool.h"

namespace Simulation
{
	/**
	 * \brief Holds a Path, Scheduler, and WorkloadPool together to form a single unit that can be simulated.
	 */
	class Experiment
	{
	public:
		/**
		 * \brief Instantiates a new, complete, experiment that starts at tick 0 and can be simulated.
		 * \param path The path this experiment should simulate. 
		 * \param scheduler The scheduler this experiment should use for workload balancing.
		 * \param pool The workloadPool that contains the workloads of this experiment.
		 * \param id The id of this experiment as it is in the database.
		 */
		Experiment(Path path, Scheduler* scheduler, WorkloadPool pool, uint32_t id) : path(path), scheduler(scheduler), id(id), currentTick(0), workloadPool(pool)
		{}

		/**
		 * \brief Simulates a single tick of this experiment.
		 */
		void tick()
		{
			if(finished) return;

			workloadPool.clearFinishedWorkloads();

			auto machineAccumulator = path.getCurrentSection(currentTick).getMachines();

			// Schedule the workload over each machine
			scheduler->schedule(machineAccumulator, workloadPool.getWorkloads(currentTick));

			// Update each machine
			std::for_each(
				machineAccumulator.begin(),
				machineAccumulator.end(),
				[this](const std::reference_wrapper<Modeling::Machine>& machineWrapper) {
					machineWrapper.get().work();
				}
			);

			currentTick++;

			if(workloadPool.isEmpty()) finished = true;
		}

		/**
		 * \brief Saves the state of the simulation, adding it to the history.
		 */
		void saveState()
		{
			for(Workload* workload : workloadPool.getWorkloads(currentTick))
			{
				history.addSnapshot(
					currentTick,
					WorkloadSnapshot(
						workload->getId(),
						workload->getRemainingOperations(),
						workload->getCoresUsed()
					)
				);
			}

			for(std::reference_wrapper<Modeling::Machine> machineref : path.getCurrentSection(currentTick).getMachines())
			{
				auto machine = machineref.get();
				history.addSnapshot(
					currentTick,
					MachineSnapshot(
						machine.getId(),
						machine.getWorkloadId(),
						machine.getTemperature(),
						machine.getLoad(),
						machine.getMemory()
					)
				);
			}
		}

		/**
		 * \brief Adds the given workload to the pool of workloads of this simulation.
		 * \param wl The workload to add to the simulator of this experiment.
		 */
		void addWorkload(Workload& wl)
		{
			workloadPool.addWorkload(wl);
		}

		/**
		 * \return A reference to the workloads of this simulation.
		 */
		WorkloadPool& getWorkloadPool()
		{
			return workloadPool;
		}

		/**
		 * \return The current tick which is being simulated, i.e. the number of ticks that have passed.
		 */
		uint32_t getCurrentTick() const
		{
			return currentTick;
		}

		/**
		 * \return The history of this experiment that has no yet been written to the database.
		 */
		SimulationHistory& getHistory()
		{
			return history;
		}

		/**
		 * \return The id of this experiment as it is in the database.
		 */
		uint32_t getId() const
		{
			return id;
		}

		/**
		 * \brief Sets this experiment to finished. After calling this method,
		 *		the tick() method will have no effect.
		 */
		void end()
		{
			this->finished = true;
		}

		/**
		 * \return True if the experiment is finished, i.e. when all workloads have been completed.
		 */
		bool isFinished() const
		{
			return this->finished;
		}

	private:
		/**
		 * \brief The path of this experiment which contains the sections and when they should be used.
		 */
		Path path;

		/**
		 * \brief The scheduler that this used for workload balancing.
		 */
		std::shared_ptr<Scheduler> scheduler;

		/**
		 * \brief The id of this experiment as it is in the database.
		 */
		uint32_t id;

		/**
		 * \brief The number of ticks that have passed.
		 */
		uint32_t currentTick;

		/**
		 * \brief The pool of workloads in this simulation, to be distributed by the scheduler.
		 */
		WorkloadPool workloadPool;

		/**
		 * \brief The part of the history of this simulation which has not been written to the database.
		 */
		SimulationHistory history;

		/**
		 * \brief If this is true, then tick will not do anything. This indicates that the simulation is finished, 
		 *		and that its history should be written to disk.
		 */
		bool finished = false;
	};
}

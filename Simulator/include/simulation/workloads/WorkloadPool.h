#pragma once
#include "simulation/workloads/Workload.h"
#include <vector>

namespace Simulation
{
	class WorkloadPool
	{
	public:
		/*
			Adds the given workload to this pool of workloads.
		*/
		void addWorkload(Workload w);

		/*
			Returns a reference to the vector of workloads.
		*/
		std::vector<Workload*> getWorkloads(uint32_t currentTick);

		/*
			Returns a reference to the workload with the given id.
		*/
		Workload& getWorkload(int id);

		/*
			Removes all workloads that are finished.
		*/
		void clearFinishedWorkloads();

		/*
			Returns true if the workloads vector of this pool is empty.
		*/
		bool isEmpty();

	private:
		/*
			Sets all dependencyFinished to true of workloads with the given id as dependency.
		*/
		void setDependenciesFinished(int id);

		std::vector<Workload> workloads;
	};
}

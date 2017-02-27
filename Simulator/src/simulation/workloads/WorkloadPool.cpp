#include "simulation/workloads/WorkloadPool.h"
#include "simulation/workloads/Workload.h"

#include <iostream>
#include <algorithm>

namespace Simulation
{
	void WorkloadPool::addWorkload(Workload w)
	{
		workloads.push_back(w);
	}

	std::vector<Workload*> WorkloadPool::getWorkloads(uint32_t currentTick)
	{
		std::vector<Workload*> filteredOnStarted;
		for(Workload& w : workloads)
		{
			if(w.getStartTick() < currentTick)
				filteredOnStarted.push_back(&w);
		}

		return filteredOnStarted;
	}

	Workload& WorkloadPool::getWorkload(int id)
	{
		auto it = std::find_if(workloads.begin(), workloads.end(), [id](Workload& w) {
			return (id == w.getId());
		});

		return *it;
	}

	void WorkloadPool::clearFinishedWorkloads()
	{
		auto it = workloads.begin();
		while(it != workloads.end())
		{
			if(it->isFinished())
			{
				std::cout << "Finished workload " << it->getId() << std::endl;
				int id = it->getId();
				setDependenciesFinished(id);
				it = workloads.erase(it);
			}
			else
			{
				++it;
			}
		}
	}

	void WorkloadPool::setDependenciesFinished(int id)
	{
		for(auto& workload : workloads)
		{
			if(workload.getDependencyId() == id)
			{
				workload.dependencyFinished = true;
				std::cout << "Finished dependency of " << workload.getId() << std::endl;
			}
		}
	}

	bool WorkloadPool::isEmpty()
	{
		return this->workloads.size() == 0;
	}
}

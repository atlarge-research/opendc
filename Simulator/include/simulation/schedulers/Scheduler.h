#pragma once
#include "simulation/workloads/Workload.h"
#include "modeling/machine/Machine.h"

#include <vector>

namespace Simulation
{
	/*
		Provides a strategy for load balancing.
	*/
	class Scheduler
	{
	public:
		virtual ~Scheduler()
		{
			
		}

		/*
			Divides the workloads over the given machines.
		*/
		virtual void schedule(std::vector<std::reference_wrapper<Modeling::Machine>>& machines, std::vector<Workload*> workloads) = 0;
	};
}

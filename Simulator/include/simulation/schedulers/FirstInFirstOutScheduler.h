#pragma once
#include "Scheduler.h"
#include <algorithm>

namespace Simulation
{
	class FirstInFirstOutScheduler : public Scheduler
	{
	protected:
		~FirstInFirstOutScheduler()
		{
		}

	public:
		/*
			Distribute workloads according to the FIFO principle
		*/
		void schedule(std::vector<std::reference_wrapper<Modeling::Machine>>& machines, std::vector<Workload*> workloads) override
		{
			if (workloads.size() == 0)
				return;

			// Find the first workload with dependencies finished
			int index = 0;
			while(!workloads.at(index)->dependencyFinished)
				index = (++index) % workloads.size();

			std::for_each(
				machines.begin(), 
				machines.end(), 
				[index, &workloads](std::reference_wrapper<Modeling::Machine>& machine) {
					machine.get().giveTask(workloads.at(index));
				}
			);
		}
	};
}

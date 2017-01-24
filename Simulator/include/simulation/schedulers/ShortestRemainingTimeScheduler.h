#pragma once
#include "Scheduler.h"
#include <algorithm>

namespace Simulation
{
	class ShortestRemainingTimeScheduler : public Scheduler
	{
	protected:
		~ShortestRemainingTimeScheduler()
		{
		}

	public:
		/*
			Distribute workloads according to the srtf principle
		*/
		void schedule(std::vector<std::reference_wrapper<Modeling::Machine>>& machines, std::vector<Workload*> workloads) override
		{
			if (workloads.size() == 0)
				return;

			std::sort(
				workloads.begin(), 
				workloads.end(), 
				[](Workload* a, Workload* b) -> bool {
					return a->getRemainingOperations() < b->getRemainingOperations();
				}
			);

			int taskIndex = 0;

			std::for_each(
				machines.begin(),
				machines.end(),
				[&workloads, &taskIndex](Modeling::Machine& machine) {
					while (!workloads.at(taskIndex)->dependencyFinished)
						taskIndex = (++taskIndex) % workloads.size();

					machine.giveTask(workloads.at(taskIndex));
					taskIndex = (++taskIndex) % workloads.size();
				}
			);
		}
	};
}

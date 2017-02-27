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
		/**
		* \brief Distribute workloads according to the srtf principle.
		*/
		void schedule(std::vector<std::reference_wrapper<Modeling::Machine>>& machines, std::vector<Workload*> workloads) override
		{
			if (workloads.size() == 0)
				return;

			std::remove_if(workloads.begin(), workloads.end(), [](Workload* workload) {
				return !workload->dependencyFinished;
			});

			for (auto workload : workloads)
			{
				workload->setCoresUsed(0);
			}

			std::sort(
				workloads.begin(), 
				workloads.end(), 
				[](Workload* a, Workload* b) -> bool {
					return a->getRemainingOperations() < b->getRemainingOperations();
				}
			);

			int taskIndex = 0;
			for (auto machine : machines)
			{
				machine.get().giveTask(workloads.at(taskIndex));
 
				workloads.at(taskIndex)->setCoresUsed(
					workloads.at(taskIndex)->getCoresUsed() + machine.get().getNumberOfCores()
				);

				if (!workloads.at(taskIndex)->isParallelizable())
				{
					workloads.erase(workloads.begin() + taskIndex);
					if (workloads.size() == 0)
						break;

					taskIndex %= workloads.size();
				}
				else
				{
					taskIndex = (++taskIndex) % workloads.size();
				}
			}
		}
	};
}

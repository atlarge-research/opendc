#include "simulation/workloads/Workload.h"

namespace Simulation
{
	Workload::Workload(int size, int startTick, int dbId, int traceId, int dependency, bool parallel) : dependencyId(dependency), remainingFlops(size), TOTAL_FLOPS(size), START_TICK(startTick), ID(dbId), TRACE_ID(traceId), isParallel(parallel) {}

	void Workload::doOperations(uint32_t opCount)
	{
		if (opCount < 0 || finished) return;

		if (remainingFlops <= opCount)
		{
			remainingFlops = 0;
			finished = true;
		}
		else
		{
			remainingFlops -= opCount;
		}
	}

	uint32_t Workload::getRemainingOperations() const
	{
		return remainingFlops;
	}

	uint32_t Workload::getTotalOperations() const
	{
		return TOTAL_FLOPS;
	}

	bool Workload::isFinished() const
	{
		return this->finished;
	}
	uint32_t Workload::getId() const
	{
		return ID;
	}

	int Workload::getDependencyId() const
	{
		return this->dependencyId;
	}

	void Workload::setCoresUsed(uint32_t cores)
	{
		this->coresUsed = cores;
	}

	uint32_t Workload::getCoresUsed()
	{
		return coresUsed;
	}

	bool Workload::isParallelizable()
	{
		return isParallel;
	}
}

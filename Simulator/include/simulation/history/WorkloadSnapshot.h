#pragma once

namespace Simulation
{
	class WorkloadSnapshot
	{
	public:
		WorkloadSnapshot(uint32_t id, uint32_t flopsDone, uint32_t coresUsed) : flopsDone(flopsDone), id(id), coresUsed(coresUsed) {}

		uint32_t flopsDone;
		uint32_t id;
		uint32_t coresUsed;
	};
}
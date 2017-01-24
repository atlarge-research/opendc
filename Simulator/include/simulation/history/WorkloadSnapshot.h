#pragma once

namespace Simulation
{
	class WorkloadSnapshot
	{
	public:
		WorkloadSnapshot(uint32_t id, uint32_t flopsDone) : flopsDone(flopsDone), id(id) {}

		uint32_t flopsDone;
		uint32_t id;
	};
}
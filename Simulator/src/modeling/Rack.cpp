#include "modeling/Rack.h"

#include <assert.h>
#include <iterator>

namespace Modeling
{
	Rack::Rack(int id, std::unordered_map<uint32_t, Machine> machines) : Entity(id), machines(machines) {}

	std::unordered_map<uint32_t, Machine>& Rack::getMachines()
	{
		return machines;
	}

	Machine& Rack::getMachineAtSlot(int slot)
	{
		assert(machines.find(slot) != machines.end());

		return machines.at(slot);
	}
}

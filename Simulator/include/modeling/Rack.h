#pragma once
#include "modeling/Entity.h"
#include "modeling/machine/Machine.h"

#include <unordered_map>

namespace Modeling
{
	/*
		The Rack class models a physical rack. It holds a vector of machines.
	*/
	class Rack : public Entity
	{
	public:
		/*
			Initializes the rack with the given machines. 
		*/
		Rack(int id, std::unordered_map<uint32_t, Machine> machines);

		/*
			Returns all machines in this rack.
		*/
		std::unordered_map<uint32_t, Machine>& getMachines();

		/*
			Returns the machine at the given slot.
		*/
		Machine& getMachineAtSlot(int slot);

	private:
		std::unordered_map<uint32_t, Machine> machines;
	};
}

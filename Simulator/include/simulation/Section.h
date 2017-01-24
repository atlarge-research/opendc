#pragma once
#include "modeling/Datacenter.h"
#include "modeling/Room.h"
#include "simulation/history/SimulationHistory.h"

#include <vector>
#include <iterator>

namespace Simulation
{
	/**
	 * \brief Holds a datacenter and the tick on which the parent experiment should switch to this section.
	 * \tparam DatacenterType The type of datacenter to be used.
	 */
	template<typename DatacenterType>
	class Section
	{
	public:
		/**
		 * \brief Initializes the datacenter in the simulation. Sets paused to false and finished to false.
		 * \param dc The topology of this section.
		 * \param startTick The tick on which the experiment should start using the topology of this section.
		 */
		Section(DatacenterType& dc, uint32_t startTick) : datacenter(dc), startTick(startTick)
		{}

		/**
		 * \return A reference to the datacenter of this section.
		 */
		DatacenterType& getDatacenter()
		{
			return datacenter;
		}

		/**
		 * \return All machines in the datacenter of section. 
		 */
		std::vector<std::reference_wrapper<Modeling::Machine>> getMachines()
		{
			using namespace std;

			vector<reference_wrapper<Modeling::Machine>> machineAccumulator;

			// For each serverroom, we get the racks in the room
			vector<Modeling::ServerRoom>& rooms = datacenter.template getRoomsOfType<Modeling::ServerRoom>();
			for(auto& room : rooms)
				// For each rack get the machines inside that rack
				for(auto& rack : room.getEntitiesOfType<Modeling::Rack>())
					// Add each machine to the accumulator
					for(auto& machine : rack.getMachines())
						machineAccumulator.push_back(ref(machine.second));

			return machineAccumulator;
		}

		/**
		 * \return The tick on which the experiment should start using the topology of this section. 
		 */
		uint32_t getStartTick() const
		{
			return startTick;
		}

	private:
		/**
		 * \brief The datacenter that is used for experiments.
		 */
		DatacenterType datacenter;

		/**
		 * \brief The tick when the next sections starts. This is -1 if this is the last section.
		 */
		uint32_t startTick;
	};

}

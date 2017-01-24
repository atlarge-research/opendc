#pragma once
#include "modeling/Room.h"
#include "simulation/schedulers/Scheduler.h"
#include "modeling/TypeIndex.h"

#include <vector>

namespace Modeling
{
	/*
		The Datacenter class models a datacenter with rooms/entities.
	*/
	template<typename ...RoomTypes>
	class Datacenter
	{
	public:
		/*
			Returns a reference to the vector of rooms in this datacenter.
		*/
		template<typename RoomType>
		std::vector<RoomType>& getRoomsOfType()
		{
			return std::get<indexOfType<RoomType, RoomTypes...>::value>(rooms);
		}

		/*
			Adds a room to this datacenter.
		*/
		template<typename RoomType>
		void addRoomOfType(RoomType& room)
		{
			std::get<indexOfType<RoomType, RoomTypes...>::value>(rooms).push_back(std::move(room));
		}

	
	private:
		// A vector of rooms that are part of this datacenter.
		std::tuple<std::vector<RoomTypes>...> rooms;
	};
}

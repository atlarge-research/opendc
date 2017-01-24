#pragma once
#include "modeling/Rack.h"
#include "modeling/TypeIndex.h"

#include <vector>

namespace Modeling
{
	/*
		The Room class models the rooms that can be created in the simulation. It contains a list of all entities in the room.
	*/
	template<typename ...EntityTypes>
	class Room
	{
		//static_assert(std::is_base_of<Entity, EntityTypes>..., "Each type must be derived from Entity!");
	public:
		/*
			Initializes the room with the given name.
		*/
		explicit Room(int id) : id(id) {}

		/*
			Adds the entity to the list of entities in this room.
		*/
		template<typename EntityType>
		void addEntity(EntityType& e)
		{
			std::get<indexOfType<EntityType, EntityTypes...>::value>(entities).push_back(e);
		}

		/*
			Returns all entities of the given type.
		*/
		template<typename EntityType>
		std::vector<EntityType>& getEntitiesOfType()
		{
			return std::get<indexOfType<EntityType, EntityTypes...>::value>(entities);
		}

		// The id of this room corresponding to its id in the database.
		const int id;

	private:
		// A vector for each type of entity
		std::tuple<std::vector<EntityTypes>...> entities;
	};

	using ServerRoom = Room<Modeling::Rack>;
	using Hallway = Room<>;
	using PowerRoom = Room<>;

}

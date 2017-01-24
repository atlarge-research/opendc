#pragma once
#include "Section.h"
#include "modeling/ModelingTypes.h"

namespace Simulation
{
	/**
	 * \brief Holds all sections of the parent experiment, and returns the correct one
	 *		based on the current tick.
	 */
	class Path
	{
	public:
		explicit Path(int id) : id(id)
		{}


		/**
		 * \brief Adds the given section to this path. The begin tick of this section 
		 *		should not already be in use by one of the other sections in this path.
		 * \param section The section to add to this path.
		 */
		void addSection(DefaultSection section)
		{
			sections.push_back(section);
		}

		/**
		 * \brief Returns the section that is currently in use by taking the section
		 *		that has the greatest begin tick smaller than the current tick 
		 * \param currentTick The tick the simulator is simulating right now.
		 * \return The section that is currently in use.
		 */
		DefaultSection& getCurrentSection(uint32_t currentTick)
		{
			size_t currentSection = 0;

			uint32_t currentStartTick = 0;
			for(int i = 0; i < sections.size(); ++i)
			{
				uint32_t tempStartTick = sections.at(i).getStartTick();
				if(tempStartTick > currentStartTick && tempStartTick < currentTick)
					currentSection = i;
			}

			return sections.at(currentSection);
		}

	private:
		
		/**
		 * \brief The unordered vector of sections in this path. No pair of sections
		 *		should share the same begin tick. 
		 */
		std::vector<DefaultSection> sections;


		/**
		 * \brief The id of this path as it is in the database.
		 */
		int id;
	};
}

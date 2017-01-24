#include "Simulator.h"
#include "modeling/ModelingTypes.h"

#include <iostream>
#include <chrono>
#include <thread>
#include <cassert>

int main(int argc, char* argv[])
{
	assert(argc == 2);

	// The main simulator, responsible for updating and writing away each simulation.
	Simulation::Simulator<DefaultSection> simulator(argv[1]);

	// Timer used for polling only once every 5 seconds
	auto pollTimer = std::chrono::high_resolution_clock::now() - std::chrono::seconds(5);

	while (true)
	{
		auto now = std::chrono::high_resolution_clock::now();

		// Calculate the time since the last polling
		std::chrono::duration<double> diff = now - pollTimer;
		if (diff.count() > 5) // Every five seconds, poll and load
		{
			// Poll and load all experiments queued in the database
			simulator.pollAndLoadAll();
			// Reset the timer for polling
			pollTimer = std::chrono::high_resolution_clock::now();
		}

		if (simulator.hasSimulations())
		{
			// Update each simulation
			simulator.tickAll();
			// Save the state of each simulation
			simulator.saveStateAll();
			// Write the history of each simulation when 500 states have been saved
			simulator.writeHistoryAll();
		}
		else // Wait for polling
		{
			std::chrono::duration<double> timeToSleep = std::chrono::seconds(5) - diff;
			std::this_thread::sleep_for(diff);
		}
	}

	// Terminal pause, press key to exit
	std::cin.get();
}

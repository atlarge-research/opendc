#pragma once

namespace Simulation {
	/*
		POD class that represents the state of a machine.
	*/
	class MachineSnapshot {
	public:
		MachineSnapshot(int id, int currentWorkload, float temp, float load, uint32_t mem) : id(id), currentWorkload(currentWorkload), temperature(temp), loadFraction(load), usedMemory(mem) {}

		int id;
		int currentWorkload;
		float temperature;
		float loadFraction;
		uint32_t usedMemory;
	};
}
#pragma once
#include "simulation/workloads/Workload.h"
#include "modeling/machine/CPU.h"
#include "modeling/machine/GPU.h"

#include <stdint.h>
#include <vector>
#include <memory>

namespace Modeling
{
	// Defines the initial temperature of machine
	constexpr float ROOM_TEMPERATURE_CELCIUS = 23.0f;

	// Defines the usage of memory by the kernel
	constexpr uint32_t KERNEL_MEMORY_USAGE_MB = 50;

	/*
		The Machine class models a physical machine in a rack. It has a speed, and can be given a workload on which it will work until finished or interrupted.
	*/
	class Machine
	{
	public:
		/*
			Initializes the machine as idle with the given speed.
		*/
		Machine(int id);

		/*
			Adds a cpu to the list of this machine.
		*/
		void addCPU(CPU cpu);

		/*
			Adds a cpu to the list of this machine.
		*/
		void addGPU(GPU gpu);
		
		/*
			Gives the task to this machine. If the machine is already busy this does nothing.
		*/
		void giveTask(Simulation::Workload* workload);

		/*
			Returns true if the machine is busy.
		*/
		bool isBusy() const;

		/*
			Does work on the given task and updates temperature and load appropriately.
		*/
		void work();

		/*
			Returns the id of the current workload of this machine.
		*/
		int getWorkloadId() const;

		/*
			Returns the id of this machine.
		*/
		int getId() const;

		/*
			Returns the temperature of this machine.
		*/
		float getTemperature() const;

		/*
			Returns the memory used by this machine.
		*/
		int getMemory() const;

		/*
			Returns the load fraction on this machine.
		*/
		float getLoad() const;

	private:
		// A list of cpus in this machine.
		std::vector<CPU> cpus;

		// A list of gpus in this machine.
		std::vector<GPU> gpus;

		// True if the machine is working on a task.
		bool busy = false;

		// The current workload the machine is working on.
		Simulation::Workload* currentWorkload;

		// Db id of this machine.
		int id;

		// Temperature of this machine.
		float temperature = ROOM_TEMPERATURE_CELCIUS;
		float maxTemperature = 80.0f;
		float minTemperature = 0.0f;
		float temperatureIncrease = 10.f;

		// Memory used by this machine.
		int memory = KERNEL_MEMORY_USAGE_MB;

		// The fraction of load on this machine.
		float load = 0.0f;

		/*
			Returns the speed of the machine.
		*/
		uint32_t getSpeed();
	};
}

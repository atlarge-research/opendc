#include "modeling/machine/Machine.h"

namespace Modeling
{
	Machine::Machine(int id) : busy(false), currentWorkload(), id(id)
	{}

	void Machine::addCPU(CPU c)
	{
		cpus.push_back(c);
	}

	void Machine::addGPU(GPU g)
	{
		gpus.push_back(g);
	}

	void Machine::giveTask(Simulation::Workload* workload)
	{
		busy = true;
		currentWorkload = workload;
	}

	bool Machine::isBusy() const
	{
		return this->busy;
	}

	uint32_t Machine::getSpeed()
	{
		int speed = 0;
		for(auto cpu : cpus)
		{
			speed += cpu.getSpeed() * cpu.getCores();
		}
		return speed;
	}

	void Machine::work()
	{
		if(!currentWorkload)
			return;

		currentWorkload->doOperations(static_cast<int>(getSpeed() * load));

		temperature += load * temperatureIncrease;
		//load = temperature < 70.0f ? 1.0f : 1.0f / (temperature - 69.0f);
		load = 1.0f;
		temperature = temperature > maxTemperature ? maxTemperature
			: temperature < minTemperature ? minTemperature
			: temperature;
	}

	int Machine::getWorkloadId() const
	{
		if(currentWorkload)
			return currentWorkload->getId();
		return 0;
	}

	int Machine::getId() const
	{
		return this->id;
	}

	float Machine::getTemperature() const
	{
		return this->temperature;
	}

	int Machine::getMemory() const
	{
		return this->memory;
	}

	float Machine::getLoad() const
	{
		return this->load;
	}

	uint32_t Machine::getNumberOfCores() const
	{
		uint32_t cores = 0;
		for (auto& processor : cpus) {
			cores += processor.getCores();
		}
		return cores;
	}
}

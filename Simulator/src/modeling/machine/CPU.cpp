#include "modeling/machine/CPU.h"

namespace Modeling
{
	CPU::CPU(int speed, int cores, int energyConsumption, int failureModelId) : speed(speed), cores(cores), energyConsumption(energyConsumption), failureModelId(failureModelId) {}

	int CPU::getCores()
	{
		return this->cores;
	}

	int CPU::getEnergyConsumption()
	{
		return this->energyConsumption;
	}

	int CPU::getFailureModelId()
	{
		return this->failureModelId;
	}

	int CPU::getSpeed()
	{
		return this->speed;
	}
}

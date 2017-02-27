#include "modeling/machine/CPU.h"

namespace Modeling
{
	CPU::CPU(int speed, int cores, int energyConsumption, int failureModelId) : speed(speed), cores(cores), energyConsumption(energyConsumption), failureModelId(failureModelId) {}

	int CPU::getCores() const
	{
		return this->cores;
	}

	int CPU::getEnergyConsumption() const
	{
		return this->energyConsumption;
	}

	int CPU::getFailureModelId() const
	{
		return this->failureModelId;
	}

	int CPU::getSpeed() const
	{
		return this->speed;
	}
}

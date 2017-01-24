#include "modeling/machine/GPU.h"

namespace Modeling
{
	GPU::GPU(int speed, int cores, int energyConsumption, int failureModelId) : speed(speed), cores(cores), energyConsumption(energyConsumption), failureModelId(failureModelId) {}

	int GPU::getCores()
	{
		return this->cores;
	}

	int GPU::getEnergyConsumption()
	{
		return this->energyConsumption;
	}

	int GPU::getFailureModelId()
	{
		return this->failureModelId;
	}

	int GPU::getSpeed()
	{
		return this->speed;
	}
}

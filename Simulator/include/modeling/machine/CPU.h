#pragma once

namespace Modeling
{
	class CPU
	{
	public:
		CPU(int speed, int cores, int energyConsumption, int failureModelId);

		/*
			Returns the speed of this CPU.
		*/
		int getSpeed();

		/*
			Returns the nr of cores of this CPU.
		*/
		int getCores();

		/*
			Returns the energy consumed by this CPU. 
		*/
		int getEnergyConsumption();

		/*
			Returns the failure model id of this CPU. 
		*/
		int getFailureModelId();


	private:
		int speed, cores, energyConsumption, failureModelId;
	};
}

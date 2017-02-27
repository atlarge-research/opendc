#pragma once

namespace Modeling
{
	class CPU
	{
	public:
		/**
		* \brief Creates a CPU with the given speed/core, number of cores, energy consumption, and failure model id.
		*/
		CPU(int speed, int cores, int energyConsumption, int failureModelId);

		/**
		* \return the speed of this CPU.
		*/
		int getSpeed() const;

		/**
		* \return The nr of cores of this CPU.
		*/
		int getCores() const;

		/**
		* \return The energy consumed by this CPU. 
		*/
		int getEnergyConsumption() const;

		/**
		* \return The failure model id of this CPU. 
		*/
		int getFailureModelId() const;


	private:
		int speed, cores, energyConsumption, failureModelId;
	};
}

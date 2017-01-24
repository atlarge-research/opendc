#pragma once
#include "simulation\SimulationHistory.h"
#include "simulation\workloads\WorkloadHistory.h"

#include <gtest\gtest.h>

TEST(SimulationHistoryTest, SetGetHistoryAtTick)
{
	Simulation::SimulationHistory simulationHistory;
	Simulation::WorkloadHistory workloadHistory;
	workloadHistory.setFlopsDone(1, 100);

	simulationHistory.setHistoryAtTick(1, workloadHistory);

	auto resultHistory = simulationHistory.getHistoryAtTick(1);
	ASSERT_EQ(resultHistory.history.at(0).first, 1);
	ASSERT_EQ(resultHistory.history.at(0).second, 100);
}

TEST(SimulationHistoryTest, ClearHistory)
{
	Simulation::SimulationHistory simulationHistory;
	Simulation::WorkloadHistory workloadHistory;
	simulationHistory.setHistoryAtTick(1, workloadHistory);

	ASSERT_EQ(simulationHistory.workloadHistories.size(), 1);

	simulationHistory.clearHistory();

	ASSERT_EQ(simulationHistory.workloadHistories.size(), 0);
}

TEST(SimulationHistoryTest, GetHistorySize)
{
	Simulation::SimulationHistory simulationHistory;
	Simulation::WorkloadHistory workloadHistory;
	simulationHistory.setHistoryAtTick(1, workloadHistory);

	ASSERT_EQ(simulationHistory.getHistorySize(), 1);

	simulationHistory.setHistoryAtTick(2, workloadHistory);

	ASSERT_EQ(simulationHistory.getHistorySize(), 2);

	simulationHistory.clearHistory();

	ASSERT_EQ(simulationHistory.getHistorySize(), 0);
}
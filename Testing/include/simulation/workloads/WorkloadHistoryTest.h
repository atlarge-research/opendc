#pragma once
#include "simulation\workloads\WorkloadHistory.h"

#include <gtest\gtest.h>

TEST(WorkloadHistoryTest, SetFlopsDone)
{
	Simulation::WorkloadHistory history;
	history.setFlopsDone(1, 5);

	auto a = history.history.at(0);

	ASSERT_EQ(a.first, 1);
	ASSERT_EQ(a.second, 5);
}
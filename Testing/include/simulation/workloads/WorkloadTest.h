#pragma once
#include "simulation\workloads\Workload.h"

#include <gtest\gtest.h>

TEST(WorkloadTest, Constructor)
{
	Simulation::Workload w(100, 0, 5, 3, 0);
	ASSERT_EQ(false, w.isFinished());
	ASSERT_EQ(5, w.getId());
	ASSERT_EQ(100, w.getRemainingOperations());
	ASSERT_EQ(100, w.getTotalOperations());
}

TEST(WorkloadTest, DoOperations)
{
	Simulation::Workload w(100, 0, 5, 3, 0);
	w.doOperations(10);
	ASSERT_EQ(90, w.getRemainingOperations());
}

TEST(WorkloadTest, GetTotalOperations)
{
	Simulation::Workload w(100, 0, 5, 3, 0);
	w.doOperations(10);
	ASSERT_EQ(100, w.getTotalOperations());
}

TEST(WorkloadTest, IsFinished)
{
	Simulation::Workload w(10, 0, 5, 3, 0);
	w.doOperations(10);
	ASSERT_EQ(true, w.isFinished());
}

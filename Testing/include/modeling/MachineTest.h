#pragma once
#include "modeling\Machine.h"

#include <gtest\gtest.h>

TEST(MachineTest, GetSpeed)
{
	Modeling::Machine m(100);

	ASSERT_EQ(m.getSpeed(), 100);
}

TEST(MachineTest, IsBusy)
{
	Modeling::Machine m(100);
	std::shared_ptr<Simulation::Workload> shrdWorkload = std::make_shared<Simulation::Workload>(150, 1, 1, 1);
	ASSERT_FALSE(m.isBusy());

	m.giveTask(std::weak_ptr<Simulation::Workload>(shrdWorkload));

	ASSERT_TRUE(m.isBusy());
}

TEST(MachineTest, Tick)
{
	Modeling::Machine m(100);
	std::shared_ptr<Simulation::Workload> shrdWorkload = std::make_shared<Simulation::Workload>(150, 1, 1, 1);
	m.giveTask(std::weak_ptr<Simulation::Workload>(shrdWorkload));

	ASSERT_TRUE(m.isBusy());

	m.tick();

	ASSERT_TRUE(m.isBusy());

	m.tick();

	ASSERT_FALSE(m.isBusy());
}
#pragma once
#include "simulation\workloads\SimpleScheduler.h"
#include "simulation\workloads\Workload.h"
#include "modeling\Machine.h"

#include <vector>
#include <gtest\gtest.h>

TEST(SimpleSchedulerTest, ScheduleSingleMachine)
{
	// Initialization
	Simulation::SimpleScheduler scheduler;
	Simulation::Workload workload1(100, 0, 1, 1, 0);
	Simulation::Workload workload2(150, 0, 1, 1, 0);
	Modeling::Machine machine(10);

	std::vector<std::reference_wrapper<Modeling::Machine>> machines;
	machines.push_back(std::reference_wrapper<Modeling::Machine>(machine));
	scheduler.addWorkload(workload1);
	scheduler.addWorkload(workload2);

	// Distribute tasks across machines
	scheduler.schedule(machines);

	// Do work
	for (auto machine : machines)
		machine.get().tick();

	// Assert work done
	auto workloads = scheduler.getWorkloads();
	auto workload1Remaining = workloads.at(0).lock()->getRemainingOperations();
	auto workload2Remaining = workloads.at(1).lock()->getRemainingOperations();
	ASSERT_EQ(workload1Remaining, 90);
	ASSERT_EQ(workload2Remaining, 150);
}

TEST(SimpleSchedulerTest, ScheduleMultipleMachine)
{
	// Initialization
	Simulation::SimpleScheduler scheduler;
	Simulation::Workload workload1(100, 0, 1, 1, 0);
	Simulation::Workload workload2(150, 0, 1, 1, 0);
	Modeling::Machine machine1(10);
	Modeling::Machine machine2(30);

	std::vector<std::reference_wrapper<Modeling::Machine>> machines;
	machines.push_back(std::reference_wrapper<Modeling::Machine>(machine1));
	machines.push_back(std::reference_wrapper<Modeling::Machine>(machine2));
	scheduler.addWorkload(workload1);
	scheduler.addWorkload(workload2);

	// Distribute tasks across machines
	scheduler.schedule(machines);

	// Do work
	for (auto machine : machines)
		machine.get().tick();

	// Assert work done
	auto workloads = scheduler.getWorkloads();
	auto workload1Remaining = workloads.at(0).lock()->getRemainingOperations();
	auto workload2Remaining = workloads.at(1).lock()->getRemainingOperations();
	ASSERT_EQ(workload1Remaining, 60);
	ASSERT_EQ(workload2Remaining, 150);
}

TEST(SimpleSchedulerTest, ScheduleFinishTask)
{
	// Initialization
	Simulation::SimpleScheduler scheduler;
	Simulation::Workload workload1(100, 0, 1, 1, 0);
	Modeling::Machine machine1(100);

	std::vector<std::reference_wrapper<Modeling::Machine>> machines;
	machines.push_back(std::reference_wrapper<Modeling::Machine>(machine1));
	scheduler.addWorkload(workload1);
	ASSERT_TRUE(scheduler.hasWorkloads());

	// Distribute tasks across machines
	scheduler.schedule(machines);

	// Do work
	for (auto machine : machines)
		machine.get().tick();

	// Distribute tasks across machines again, this is when finished workloads get cleared
	scheduler.schedule(machines);

	// Assert work done
	auto workloads = scheduler.getWorkloads();
	ASSERT_EQ(workloads.size(), 0);
	ASSERT_FALSE(scheduler.hasWorkloads());
}

TEST(SimpleSchedulerTest, AddMultipleWorkloads)
{
	Simulation::SimpleScheduler ss;
	std::vector<Simulation::Workload> workloads{
		Simulation::Workload(100, 0, 1, 1, 0),
		Simulation::Workload(100, 0, 1, 1, 0)
	};
	ss.addWorkloads(workloads);

	ASSERT_TRUE(ss.hasWorkloads());
	ASSERT_EQ(ss.getWorkloads().size(), 2);
}
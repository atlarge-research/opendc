#pragma once
#include "modeling\Datacenter.h"
#include "simulation\workloads\SimpleScheduler.h"

#include <gtest\gtest.h>

TEST(DatacenterTest, GetAddRoomOfType)
{
	Simulation::Scheduler* scheduler = new Simulation::SimpleScheduler();
	Modeling::Datacenter<int, float, double> datacenter(scheduler);

	int first = 4, second = 1;
	datacenter.addRoomOfType<int>(first);
	datacenter.addRoomOfType<int>(second);
	double third = 3.0;
	datacenter.addRoomOfType<double>(third);

	ASSERT_EQ(datacenter.getRoomsOfType<int>().at(0), 4);
	ASSERT_EQ(datacenter.getRoomsOfType<int>().at(1), 1);
	ASSERT_EQ(datacenter.getRoomsOfType<double>().at(0), 3.0);
}

TEST(DatacenterTest, GetSetScheduler)
{
	Simulation::Scheduler* scheduler = new Simulation::SimpleScheduler();
	Simulation::Scheduler* secondScheduler = new Simulation::SimpleScheduler();
	
	Modeling::Datacenter<int, float, double> datacenter(scheduler);

	ASSERT_EQ(datacenter.getScheduler().get(), scheduler);

	datacenter.setScheduler(secondScheduler);

	ASSERT_EQ(datacenter.getScheduler().get(), secondScheduler);
}
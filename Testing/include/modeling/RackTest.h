#pragma once
#include "modeling\Rack.h"

#include <gtest\gtest.h>

TEST(RackTest, ConstructorTest) 
{
	Modeling::Rack rack(10, 100);
	ASSERT_EQ(rack.id, 10);
}

TEST(RackTest, GetSetMachines)
{
	Modeling::Rack rack(10, 100);

	Modeling::Machine machine(100);
	rack.setMachine(machine, 10);

	ASSERT_EQ(rack.getMachines().size(), 1);
	ASSERT_EQ(rack.getMachines().at(0).get().getSpeed(), 100);
}

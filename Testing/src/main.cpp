#include "simulation\workloads\WorkloadTest.h"
#include "simulation\workloads\WorkloadHistoryTest.h"
#include "simulation\workloads\SimpleSchedulerTest.h"
#include "simulation\SimulationHistoryTest.h"
#include "modeling\TypeIndexTest.h"
#include "modeling\MachineTest.h"
#include "modeling\EntityTest.h"
#include "modeling\DatacenterTest.h"
#include "modeling\RackTest.h"
#include "modeling\RoomTest.h"

#include <gtest/gtest.h>

int main(int ac, char* av[])
{
	testing::InitGoogleTest(&ac, av);
	int rc = RUN_ALL_TESTS();
	std::cin.get();
	return rc;
}


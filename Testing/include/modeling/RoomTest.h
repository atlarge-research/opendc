#pragma once
#include "modeling\Room.h"

#include <gtest\gtest.h>

TEST(RoomTest, ConstructorTest)
{
	Modeling::Room<int, float, double> room(10);
	ASSERT_EQ(room.id, 10);
}

TEST(RoomTest, GetSetEntities)
{
	Modeling::Room<int, float, double> room(10);

	int first = 3;
	room.addEntity(first);

	double second = 4.0;
	room.addEntity(second);

	ASSERT_EQ(room.getEntitiesOfType<int>().at(0), 3);
	ASSERT_EQ(room.getEntitiesOfType<double>().at(0), 4.0);
}

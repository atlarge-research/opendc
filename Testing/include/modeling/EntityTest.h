#pragma once
#include "modeling\Entity.h"

#include <gtest\gtest.h>

TEST(EntityTest, Constructor)
{
	Modeling::Entity e(10);
	ASSERT_EQ(e.id, 10);
}